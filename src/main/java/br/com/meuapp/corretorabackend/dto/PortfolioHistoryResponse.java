package br.com.meuapp.corretorabackend.dto;

import java.util.List;

public record PortfolioHistoryResponse(
        List<String> labels,
        List<Double> carteira,
        List<Double> cdi
) {}
