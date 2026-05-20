package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diseases", uniqueConstraints = {
        @UniqueConstraint(columnNames = "disease_code", name = "uk_disease_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disease_id")
    private Integer diseaseId;

    @Column(name = "disease_code", nullable = false, length = 100)
    private String diseaseCode;

    @Column(name = "disease_name_vi", nullable = false, length = 255)
    private String diseaseNameVi;

    @Column(name = "disease_name_en", length = 255)
    private String diseaseNameEn;

    @Column(name = "icd10_code", length = 20)
    private String icd10Code;

    @Column(length = 50)
    private String category;

    @Builder.Default
    @Column(columnDefinition = "ENUM('LOW','MEDIUM','HIGH','CRITICAL') DEFAULT 'MEDIUM'")
    private String severity = "MEDIUM";

    @Builder.Default
    @Column(name = "is_cancer")
    private Boolean isCancer = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(columnDefinition = "TEXT")
    private String recommendation;
}
