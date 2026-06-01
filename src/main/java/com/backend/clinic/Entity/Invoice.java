package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices", uniqueConstraints = {
        @UniqueConstraint(columnNames = "invoice_code", name = "uk_invoice_code"),
        @UniqueConstraint(columnNames = "record_id", name = "uk_invoice_record")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_code", nullable = false, length = 20)
    private String invoiceCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord medicalRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Builder.Default
    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "medication_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal medicationFee = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "other_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal otherFee = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    // Generated Column: consultation_fee + medication_fee + other_fee - discount
    @Column(name = "total_amount", insertable = false, updatable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "stripe_payment_intent_id", length = 100)
    private String stripePaymentIntentId;

    @Builder.Default
    @Column(name = "payment_status", columnDefinition = "ENUM('UNPAID','PAID','PARTIAL','REFUNDED','CANCELLED') DEFAULT 'UNPAID'")
    private String paymentStatus = "UNPAID";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
