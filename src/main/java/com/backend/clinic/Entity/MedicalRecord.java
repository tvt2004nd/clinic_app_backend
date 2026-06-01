package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "medical_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = "record_code", name = "uk_record_code"),
        @UniqueConstraint(columnNames = "appointment_id", name = "uk_record_appt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "record_code", nullable = false, length = 20)
    private String recordCode;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "appointment_id", nullable = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String symptoms;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_diagnosis_id")
    private AiDiagnosis aiDiagnosis;

    @Column(name = "final_diagnosis", nullable = false, columnDefinition = "TEXT")
    private String finalDiagnosis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_disease_id")
    private Disease finalDisease;

    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "lesion_description", columnDefinition = "TEXT")
    private String lesionDescription;

    @Column(name = "lesion_locations", columnDefinition = "TEXT")
    private String lesionLocations;

    @Column(name = "lesion_features", columnDefinition = "TEXT")
    private String lesionFeatures;

    @Column(name = "lesion_color", length = 50)
    private String lesionColor;

    @Column(name = "lesion_size_cm")
    private BigDecimal lesionSizeCm;

    @Column(name = "lesion_shape", length = 100)
    private String lesionShape;

    @Column(name = "explained_to_patient", nullable = false)
    private boolean explainedToPatient = false;

    @Column(name = "followup_scheduled", nullable = false)
    private boolean followupScheduled = false;

    @CreationTimestamp
    @Column(name = "examined_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime examinedAt;
}
