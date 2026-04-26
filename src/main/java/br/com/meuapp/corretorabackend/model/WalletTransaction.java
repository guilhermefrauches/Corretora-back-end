package br.com.meuapp.corretorabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Data
@NoArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    private String description;

    @Column(name = "stripe_payment_id")
    private String stripePaymentId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        BUY,
        SELL
    }
}