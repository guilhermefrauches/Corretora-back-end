package br.com.meuapp.corretorabackend.service;

import br.com.meuapp.corretorabackend.dto.BrapiQuoteResponse;
import br.com.meuapp.corretorabackend.model.Position;
import br.com.meuapp.corretorabackend.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PositionPriceScheduler {

    private final PositionRepository positionRepository;
    private final BrapiService brapiService;

    @Scheduled(fixedDelay = 5 * 60 * 1000) // a cada 5 minutos
    public void updatePrices() {
        List<String> tickers = positionRepository.findDistinctTickers();
        if (tickers.isEmpty()) return;

        log.info("Atualizando preços para {} ticker(s): {}", tickers.size(), tickers);

        for (String ticker : tickers) {
            try {
                BrapiQuoteResponse response = brapiService.getQuote(ticker);
                if (response.getResults() == null || response.getResults().isEmpty()) continue;

                BigDecimal price = response.getResults().get(0).getRegularMarketPrice();
                if (price == null) continue;

                List<Position> positions = positionRepository.findByTicker(ticker);
                positions.forEach(p -> p.setCurrentPrice(price));
                positionRepository.saveAll(positions);

                log.info("Preço atualizado: {} = R$ {}", ticker, price);
            } catch (Exception e) {
                log.warn("Falha ao atualizar preço de {}: {}", ticker, e.getMessage());
            }
        }
    }
}
