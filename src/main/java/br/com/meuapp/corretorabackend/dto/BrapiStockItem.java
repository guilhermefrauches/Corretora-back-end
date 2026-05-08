package br.com.meuapp.corretorabackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrapiStockItem {

    private String stock;
    private String name;
    private BigDecimal close;
    private Double change;
    private Long volume;

    @JsonProperty("market_cap_basic")
    private Long marketCapBasic;

    private String sector;
    private String type;
}
