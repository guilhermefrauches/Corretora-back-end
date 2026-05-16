package br.com.meuapp.corretorabackend.service;

import br.com.meuapp.corretorabackend.dto.BrapiQuoteResponse;
import br.com.meuapp.corretorabackend.dto.BrapiQuoteResult;
import br.com.meuapp.corretorabackend.dto.HistoricalDataPrice;
import br.com.meuapp.corretorabackend.dto.PortfolioHistoryResponse;
import br.com.meuapp.corretorabackend.model.Trade;
import br.com.meuapp.corretorabackend.model.User;
import br.com.meuapp.corretorabackend.repository.TradeRepository;
import br.com.meuapp.corretorabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioHistoryService {

    private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final double FALLBACK_CDI_ANNUAL = 10.4;

    private final UserRepository userRepository;
    private final TradeRepository tradeRepository;
    private final BrapiService brapiService;

    public PortfolioHistoryResponse getHistory(String email, String period) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        List<Trade> trades = tradeRepository.findByUserOrderByExecutedAtAsc(user);
        if (trades.isEmpty()) {
            return empty();
        }

        String[] rangeInterval = toRangeInterval(period);
        String brapiRange = rangeInterval[0];
        String brapiInterval = rangeInterval[1];

        Set<String> tickers = trades.stream().map(Trade::getTicker).collect(Collectors.toCollection(LinkedHashSet::new));

        // Free brapi plan: 1 ticker per request
        Map<String, TreeMap<LocalDate, BigDecimal>> pricesByTicker = new HashMap<>();
        for (String ticker : tickers) {
            try {
                BrapiQuoteResponse response = brapiService.getQuoteWithHistory(ticker, brapiRange, brapiInterval);
                if (response != null && response.getResults() != null) {
                    pricesByTicker.putAll(buildPriceMap(response.getResults()));
                }
            } catch (Exception e) {
                log.warn("Brapi historical fetch failed for [{}]: {}", ticker, e.getMessage());
            }
        }

        // Merge all dates from all tickers
        TreeSet<LocalDate> allDates = pricesByTicker.values().stream()
                .flatMap(m -> m.keySet().stream())
                .collect(Collectors.toCollection(TreeSet::new));

        if (allDates.isEmpty()) {
            return empty();
        }

        List<LocalDate> datePoints = new ArrayList<>(allDates);

        // Calculate portfolio cumulative return at each date
        List<Double> carteiraReturns = buildCarteiraReturns(trades, pricesByTicker, datePoints);

        // Calculate CDI cumulative return at each date
        List<Double> cdiReturns = buildCdiReturns(datePoints);

        List<String> labels = datePoints.stream()
                .map(d -> d.format(LABEL_FMT))
                .toList();

        return new PortfolioHistoryResponse(labels, carteiraReturns, cdiReturns);
    }

    // --- private helpers ---

    private Map<String, TreeMap<LocalDate, BigDecimal>> buildPriceMap(List<BrapiQuoteResult> results) {
        Map<String, TreeMap<LocalDate, BigDecimal>> map = new HashMap<>();
        for (BrapiQuoteResult result : results) {
            if (result.getHistoricalDataPrice() == null) continue;
            TreeMap<LocalDate, BigDecimal> prices = new TreeMap<>();
            for (HistoricalDataPrice hdp : result.getHistoricalDataPrice()) {
                if (hdp.getDate() == null || hdp.getClose() == null) continue;
                LocalDate date = Instant.ofEpochSecond(hdp.getDate()).atZone(ZONE_BR).toLocalDate();
                prices.put(date, BigDecimal.valueOf(hdp.getClose()));
            }
            if (!prices.isEmpty()) {
                map.put(result.getSymbol(), prices);
            }
        }
        return map;
    }

    private List<Double> buildCarteiraReturns(List<Trade> trades,
                                               Map<String, TreeMap<LocalDate, BigDecimal>> pricesByTicker,
                                               List<LocalDate> datePoints) {
        List<Double> returns = new ArrayList<>();
        BigDecimal initialValue = null;

        for (LocalDate date : datePoints) {
            BigDecimal value = portfolioValueAt(trades, pricesByTicker, date);

            if (initialValue == null && value.compareTo(BigDecimal.ZERO) > 0) {
                initialValue = value;
            }

            double returnPct = 0.0;
            if (initialValue != null && initialValue.compareTo(BigDecimal.ZERO) > 0) {
                returnPct = value.subtract(initialValue)
                        .divide(initialValue, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }
            returns.add(round2(returnPct));
        }

        return returns;
    }

    private BigDecimal portfolioValueAt(List<Trade> trades,
                                         Map<String, TreeMap<LocalDate, BigDecimal>> pricesByTicker,
                                         LocalDate date) {
        Map<String, BigDecimal> quantities = new HashMap<>();
        for (Trade trade : trades) {
            if (trade.getExecutedAt().toLocalDate().isAfter(date)) break; // sorted asc
            String ticker = trade.getTicker();
            BigDecimal qty = quantities.getOrDefault(ticker, BigDecimal.ZERO);
            quantities.put(ticker, trade.getType() == Trade.TradeType.BUY
                    ? qty.add(trade.getQuantity())
                    : qty.subtract(trade.getQuantity()));
        }

        BigDecimal value = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : quantities.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) <= 0) continue;
            TreeMap<LocalDate, BigDecimal> prices = pricesByTicker.get(entry.getKey());
            if (prices == null) continue;
            Map.Entry<LocalDate, BigDecimal> floor = prices.floorEntry(date);
            if (floor == null) continue;
            value = value.add(floor.getValue().multiply(entry.getValue()));
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private List<Double> buildCdiReturns(List<LocalDate> datePoints) {
        double annualCdi = fetchAnnualCdi();
        double dailyCdi = Math.pow(1 + annualCdi / 100.0, 1.0 / 252.0) - 1;
        LocalDate start = datePoints.get(0);

        return datePoints.stream()
                .map(date -> {
                    long days = ChronoUnit.DAYS.between(start, date);
                    double cdi = (Math.pow(1 + dailyCdi, days) - 1) * 100.0;
                    return round2(cdi);
                })
                .toList();
    }

    private double fetchAnnualCdi() {
        try {
            var response = brapiService.getPrimeRate("brazil", false, null, null);
            if (response.getPrimeRate() != null && !response.getPrimeRate().isEmpty()) {
                Double value = response.getPrimeRate().get(0).getValue();
                if (value != null && value > 0) return value;
            }
        } catch (Exception e) {
            log.warn("Could not fetch CDI rate, using fallback {}%", FALLBACK_CDI_ANNUAL);
        }
        return FALLBACK_CDI_ANNUAL;
    }

    private static String[] toRangeInterval(String period) {
        return switch (period.toUpperCase()) {
            case "1M"  -> new String[]{"1mo", "1wk"};
            case "6M"  -> new String[]{"6mo", "1wk"};
            case "1A"  -> new String[]{"1y",  "1mo"};
            case "MAX" -> new String[]{"5y",  "3mo"};
            default    -> new String[]{"3mo", "1wk"}; // 3M default
        };
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static PortfolioHistoryResponse empty() {
        return new PortfolioHistoryResponse(List.of(), List.of(), List.of());
    }
}
