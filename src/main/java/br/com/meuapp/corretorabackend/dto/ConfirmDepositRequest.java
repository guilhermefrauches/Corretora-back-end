package br.com.meuapp.corretorabackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmDepositRequest {

    @NotBlank(message = "paymentIntentId é obrigatório")
    private String paymentIntentId;
}
