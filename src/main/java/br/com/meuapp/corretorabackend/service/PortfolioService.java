package br.com.meuapp.corretorabackend.service;

import br.com.meuapp.corretorabackend.dto.BuyRequest;
import br.com.meuapp.corretorabackend.dto.PortfolioResponse;
import br.com.meuapp.corretorabackend.dto.PositionResponse;
import br.com.meuapp.corretorabackend.dto.SellRequest;
import br.com.meuapp.corretorabackend.model.Position;
import br.com.meuapp.corretorabackend.model.Trade;
import br.com.meuapp.corretorabackend.model.User;
import br.com.meuapp.corretorabackend.repository.PositionRepository;
import br.com.meuapp.corretorabackend.repository.TradeRepository;
import br.com.meuapp.corretorabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final TradeRepository tradeRepository;

    @Transactional
    public PortfolioResponse buy(String email, BuyRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        BigDecimal totalCost = request.getQuantity().multiply(request.getPrice());

        BigDecimal balance = walletService.getOrCreateWallet(user).getBalance();
        if (balance.compareTo(totalCost) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        String ticker = request.getTicker().toUpperCase();
        String description = String.format("Compra de %s %s a R$ %s",
                request.getQuantity().stripTrailingZeros().toPlainString(),
                ticker,
                request.getPrice());
        walletService.debitForTrade(user, totalCost, description);

        Trade trade = new Trade();
        trade.setUser(user);
        trade.setTicker(ticker);
        trade.setQuantity(request.getQuantity());
        trade.setPrice(request.getPrice());
        trade.setType(Trade.TradeType.BUY);
        trade.setAssetType(request.getAssetType());
        tradeRepository.save(trade);

        Optional<Position> existing = positionRepository.findByUserAndTicker(user, ticker);
        if (existing.isPresent()) {
            Position position = existing.get();
            BigDecimal newQuantity = position.getQuantity().add(request.getQuantity());
            BigDecimal newAveragePrice = position.getQuantity().multiply(position.getAveragePrice())
                    .add(request.getQuantity().multiply(request.getPrice()))
                    .divide(newQuantity, 8, RoundingMode.HALF_UP);
            position.setQuantity(newQuantity);
            position.setAveragePrice(newAveragePrice);
            position.setUpdatedAt(LocalDateTime.now());
            positionRepository.save(position);
        } else {
            Position position = new Position();
            position.setUser(user);
            position.setTicker(ticker);
            position.setQuantity(request.getQuantity());
            position.setAveragePrice(request.getPrice());
            position.setAssetType(request.getAssetType());
            positionRepository.save(position);
        }

        return buildPortfolio(user);
    }

    @Transactional
    public PortfolioResponse sell(String email, SellRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        String ticker = request.getTicker().toUpperCase();
        Position position = positionRepository.findByUserAndTicker(user, ticker)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade insuficiente"));

        if (position.getQuantity().compareTo(request.getQuantity()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade insuficiente");
        }

        BigDecimal totalValue = request.getQuantity().multiply(request.getPrice());
        String description = String.format("Venda de %s %s a R$ %s",
                request.getQuantity().stripTrailingZeros().toPlainString(),
                ticker,
                request.getPrice());
        walletService.creditForTrade(user, totalValue, description);

        Trade trade = new Trade();
        trade.setUser(user);
        trade.setTicker(ticker);
        trade.setQuantity(request.getQuantity());
        trade.setPrice(request.getPrice());
        trade.setType(Trade.TradeType.SELL);
        tradeRepository.save(trade);

        if (position.getQuantity().compareTo(request.getQuantity()) == 0) {
            positionRepository.delete(position);
        } else {
            position.setQuantity(position.getQuantity().subtract(request.getQuantity()));
            position.setUpdatedAt(LocalDateTime.now());
            positionRepository.save(position);
        }

        return buildPortfolio(user);
    }

    public PortfolioResponse getPortfolio(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        return buildPortfolio(user);
    }

    private PortfolioResponse buildPortfolio(User user) {
        List<Position> rawPositions = positionRepository.findByUser(user);
        BigDecimal walletBalance = walletService.getOrCreateWallet(user).getBalance();

        if (rawPositions.isEmpty()) {
            return new PortfolioResponse(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    new PortfolioResponse.Wallet(walletBalance));
        }

        List<PositionResponse> positions = rawPositions.stream()
                .map(p -> {
                    BigDecimal currentPrice = p.getCurrentPrice() != null ? p.getCurrentPrice() : p.getAveragePrice();
                    BigDecimal totalValue = currentPrice.multiply(p.getQuantity()).setScale(2, RoundingMode.HALF_UP);
                    return new PositionResponse(p.getTicker(), p.getAssetType(), p.getQuantity(), p.getAveragePrice(), currentPrice, totalValue);
                })
                .toList();

        BigDecimal totalValue = positions.stream()
                .map(PositionResponse::totalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal costBasis = rawPositions.stream()
                .map(p -> p.getAveragePrice().multiply(p.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal allTimeReturnBRL = totalValue.subtract(costBasis).setScale(2, RoundingMode.HALF_UP);

        BigDecimal allTimeReturnPct = costBasis.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : allTimeReturnBRL.divide(costBasis, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);

        return new PortfolioResponse(positions, totalValue, allTimeReturnBRL, allTimeReturnPct,
                new PortfolioResponse.Wallet(walletBalance));
    }
}
