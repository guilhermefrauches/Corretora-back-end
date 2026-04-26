package br.com.meuapp.corretorabackend.service;

import br.com.meuapp.corretorabackend.dto.BuyRequest;
import br.com.meuapp.corretorabackend.dto.PortfolioResponse;
import br.com.meuapp.corretorabackend.dto.PositionResponse;
import br.com.meuapp.corretorabackend.dto.SellRequest;
import br.com.meuapp.corretorabackend.model.Position;
import br.com.meuapp.corretorabackend.model.User;
import br.com.meuapp.corretorabackend.repository.PositionRepository;
import br.com.meuapp.corretorabackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PositionRepository positionRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

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
        List<PositionResponse> positions = positionRepository.findByUser(user).stream()
                .map(p -> new PositionResponse(
                        p.getTicker(),
                        p.getQuantity(),
                        p.getAveragePrice(),
                        null,
                        p.getQuantity().multiply(p.getAveragePrice()).setScale(2, RoundingMode.HALF_UP)
                ))
                .toList();

        BigDecimal totalValue = positions.stream()
                .map(PositionResponse::totalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortfolioResponse(positions, totalValue);
    }
}
