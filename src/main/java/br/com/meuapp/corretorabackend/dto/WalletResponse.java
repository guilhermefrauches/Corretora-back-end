package br.com.meuapp.corretorabackend.dto;

import br.com.meuapp.corretorabackend.model.WalletTransaction;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class WalletResponse {

    private Long id;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private List<TransactionResponse> transactions;

    @Data
    @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private WalletTransaction.TransactionType type;
        private BigDecimal amount;
        private String description;
        private LocalDateTime createdAt;
    }
}