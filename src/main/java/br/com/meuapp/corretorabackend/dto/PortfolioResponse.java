package br.com.meuapp.corretorabackend.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
        List<PositionResponse> positions,
        BigDecimal totalValue,
        BigDecimal allTimeReturnBRL,
        BigDecimal allTimeReturnPct,
        Wallet wallet
) {
    public record Wallet(BigDecimal balance) {}
}
