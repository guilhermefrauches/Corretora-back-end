package br.com.meuapp.corretorabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentIntentResponse {

    private String clientSecret;
    private String publicKey;
    private String paymentIntentId;
    private Long amount;
    private String currency;
    private String status;
}