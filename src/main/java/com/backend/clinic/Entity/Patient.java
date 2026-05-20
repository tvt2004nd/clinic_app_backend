package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "patients", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id", name = "uk_patients_user"),
        @UniqueConstraint(columnNames = "patient_code", name = "uk_patients_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long patientId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "patient_code", nullable = false, length = 20)
    private String patientCode;

    @Builder.Default
    @Column(name = "blood_type", columnDefinition = "ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-','UNKNOWN') DEFAULT 'UNKNOWN'")
    private String bloodType = "UNKNOWN";

    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    @Column(name = "insurance_number", length = 50)
    private String insuranceNumber;

    @Column(name = "emergency_contact", length = 100)
    private String emergencyContact;

    @Column(name = "emergency_phone", length = 15)
    private String emergencyPhone;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
