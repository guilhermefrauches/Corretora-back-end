package br.com.meuapp.corretorabackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BuyRequest {

    @NotBlank
    private String ticker;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal quantity;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;
}