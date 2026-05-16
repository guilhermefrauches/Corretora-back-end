package br.com.meuapp.corretorabackend.dto;

import java.math.BigDecimal;

public record PositionResponse(
        String ticker,
        String assetType,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal totalValue
) {}
