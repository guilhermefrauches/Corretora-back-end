package br.com.meuapp.corretorabackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrapiQuoteResult {

    private String symbol;
    private String shortName;
    private BigDecimal regularMarketPrice;
    private BigDecimal regularMarketChange;
    private Double regularMarketChangePercent;
    private Long regularMarketVolume;
    private BigDecimal regularMarketDayHigh;
    private BigDecimal regularMarketDayLow;
    private BigDecimal regularMarketPreviousClose;
    private Long marketCap;
    private String currency;
}
