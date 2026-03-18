package br.com.meuapp.corretorabackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WithdrawRequest {

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mínimo de saque é R$ 0,01")
    private BigDecimal amount;

    private String description;
}