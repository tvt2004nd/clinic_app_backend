package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(columnNames = "payment_code", name = "uk_payments_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "payment_code", nullable = false, length = 30)
    private String paymentCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "payment_method", columnDefinition = "ENUM('CASH','BANK_TRANSFER','MOMO','VNPAY','ZALOPAY','CREDIT_CARD') DEFAULT 'CASH'")
    private String paymentMethod = "CASH";

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Builder.Default
    @Column(columnDefinition = "ENUM('PENDING','SUCCESS','FAILED','REFUNDED') DEFAULT 'PENDING'")
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "paid_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime paidAt;
}
