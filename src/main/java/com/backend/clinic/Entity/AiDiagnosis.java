package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_diagnoses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiDiagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diagnosis_id")
    private Long diagnosisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "top1_disease_id")
    private Disease top1Disease;

    @Column(name = "top1_confidence", precision = 5, scale = 4)
    private BigDecimal top1Confidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "top2_disease_id")
    private Disease top2Disease;

    @Column(name = "top2_confidence", precision = 5, scale = 4)
    private BigDecimal top2Confidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "top3_disease_id")
    private Disease top3Disease;

    @Column(name = "top3_confidence", precision = 5, scale = 4)
    private BigDecimal top3Confidence;

    @Column(name = "confidence_level", columnDefinition = "ENUM('HIGH_CONSENSUS','HIGH_SINGLE','MEDIUM','LOW')")
    private String confidenceLevel;

    @Builder.Default
    @Column(name = "model_consensus")
    private Integer modelConsensus = 1;

    @Builder.Default
    @Column(name = "has_cancer_warning")
    private Boolean hasCancerWarning = false;

    @Builder.Default
    @Column(name = "is_confirmed")
    private Boolean isConfirmed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Doctor confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "doctor_note", columnDefinition = "TEXT")
    private String doctorNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
