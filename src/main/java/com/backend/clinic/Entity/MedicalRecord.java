package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
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

    @CreationTimestamp
    @Column(name = "examined_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime examinedAt;
}
