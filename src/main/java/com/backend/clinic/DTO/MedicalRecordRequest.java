package com.backend.clinic.DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MedicalRecordRequest {
    private String recordCode;
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private String symptoms;
    private Long aiDiagnosisId;
    private String finalDiagnosis;
    private Integer finalDiseaseId;
    private String treatmentPlan;
    private LocalDate followUpDate;

    private String lesionDescription;
    private String lesionLocations; // JSON or CSV
    private String lesionFeatures; // JSON or CSV
    private String lesionColor;
    private BigDecimal lesionSizeCm;
    private String lesionShape;

    private Boolean explainedToPatient;
    private Boolean followupScheduled;

    private List<String> imageUrls;
}
