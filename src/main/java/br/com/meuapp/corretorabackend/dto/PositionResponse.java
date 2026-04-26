package br.com.meuapp.corretorabackend.dto;

import java.math.BigDecimal;

public record PositionResponse(
        String ticker,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal totalValue
) {}
