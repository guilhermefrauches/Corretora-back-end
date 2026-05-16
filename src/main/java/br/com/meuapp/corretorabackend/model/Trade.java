package br.com.meuapp.corretorabackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Data
@NoArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false, precision = 15, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 15, scale = 8)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(name = "asset_type")
    private String assetType; // "acao" ou "fii"

    @Column(name = "executed_at")
    private LocalDateTime executedAt = LocalDateTime.now();

    public enum TradeType {
        BUY, SELL
    }
}
