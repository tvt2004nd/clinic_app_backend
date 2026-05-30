package com.backend.clinic.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ExaminationDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IntakeRequest {
        private Long appointmentId;
        private Long patientId;
        private Long doctorId;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;

        @Size(max = 500)
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymptomsUpdateRequest {
        @NotBlank(message = "Symptoms cannot be empty")
        private String symptoms;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinalDiagnosisUpdateRequest {
        @NotBlank(message = "Final diagnosis cannot be empty")
        private String finalDiagnosis;
        private Integer finalDiseaseId;
        private String treatmentPlan;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrescriptionItemRequest {
        @NotNull(message = "Medication ID is required")
        private Integer medicationId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;

        @Size(max = 255)
        private String dosageInstruction;

        @Min(value = 1, message = "Duration days must be at least 1")
        private Integer durationDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrescriptionUpdateRequest {
        @Valid
        @NotNull(message = "Prescription items cannot be null")
        private List<PrescriptionItemRequest> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FollowUpRequest {
        @NotNull(message = "Follow-up date is required")
        private LocalDate followUpDate;
        private LocalTime followUpTime;
        private Long doctorId;

        @Size(max = 500)
        private String reason;

        @Builder.Default
        private Boolean createAppointment = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PatientLookupResponse {
        private Long patientId;
        private String patientCode;
        private String fullName;
        private String phone;
        private String gender;
        private LocalDate dateOfBirth;
        private String insuranceNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppointmentQueueItemResponse {
        private Long appointmentId;
        private String appointmentCode;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private String status;
        private String reason;
        private Long patientId;
        private String patientCode;
        private String patientName;
        private String patientPhone;
        private Long doctorId;
        private String doctorName;
        private Long clinicRoomId;
        private String clinicRoomCode;
        private String clinicRoomName;
        private Long medicalRecordId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DiseaseLookupResponse {
        private Integer diseaseId;
        private String diseaseCode;
        private String diseaseNameVi;
        private String diseaseNameEn;
        private String icd10Code;
        private String severity;
        private Boolean isCancer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicationLookupResponse {
        private Integer medicationId;
        private String medCode;
        private String medName;
        private String categoryName;
        private String activeIngredient;
        private String dosageForm;
        private String unit;
        private BigDecimal price;
        private Integer stockQuantity;
        private Boolean isActive;
        private String medicationType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppointmentSummaryResponse {
        private Long appointmentId;
        private String appointmentCode;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private String status;
        private String reason;
        private Long clinicRoomId;
        private String clinicRoomCode;
        private String clinicRoomName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PatientSummaryResponse {
        private Long patientId;
        private String patientCode;
        private String fullName;
        private String phone;
        private String gender;
        private LocalDate dateOfBirth;
        private String address;
        private String bloodType;
        private String insuranceNumber;
        private String medicalHistory;
        private String emergencyContact;
        private String emergencyPhone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorSummaryResponse {
        private Long doctorId;
        private String doctorCode;
        private String fullName;
        private String title;
        private String specialtyName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DiseaseSummaryResponse {
        private Integer diseaseId;
        private String diseaseCode;
        private String diseaseNameVi;
        private String diseaseNameEn;
        private String icd10Code;
        private String severity;
        private Boolean isCancer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiDiagnosisSummaryResponse {
        private Long diagnosisId;
        private String imagePath;
        private DiseaseSummaryResponse top1Disease;
        private BigDecimal top1Confidence;
        private DiseaseSummaryResponse top2Disease;
        private BigDecimal top2Confidence;
        private DiseaseSummaryResponse top3Disease;
        private BigDecimal top3Confidence;
        private String confidenceLevel;
        private Integer modelConsensus;
        private Boolean hasCancerWarning;
        private Boolean isConfirmed;
        private Long confirmedByDoctorId;
        private String confirmedByDoctorName;
        private LocalDateTime confirmedAt;
        private String doctorNote;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AllergySummaryResponse {
        private Long allergyId;
        private String allergen;
        private String reaction;
        private String severity;
        private String note;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrescriptionItemResponse {
        private Long itemId;
        private Integer medicationId;
        private String medCode;
        private String medName;
        private String unit;
        private Integer quantity;
        private String dosageInstruction;
        private Integer durationDays;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FollowUpAppointmentResponse {
        private Long appointmentId;
        private String appointmentCode;
        private LocalDate appointmentDate;
        private LocalTime appointmentTime;
        private String status;
        private Long doctorId;
        private String doctorName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicalRecordDetailResponse {
        private Long recordId;
        private String recordCode;
        private AppointmentSummaryResponse appointment;
        private PatientSummaryResponse patient;
        private DoctorSummaryResponse doctor;
        private String symptoms;
        private AiDiagnosisSummaryResponse selectedAiDiagnosis;
        private List<AiDiagnosisSummaryResponse> aiDiagnoses;
        private String finalDiagnosis;
        private DiseaseSummaryResponse finalDisease;
        private String treatmentPlan;
        private LocalDate followUpDate;
        private List<AllergySummaryResponse> allergies;
        private List<PrescriptionItemResponse> prescriptionItems;
        private LocalDateTime examinedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FollowUpScheduleResponse {
        private MedicalRecordDetailResponse medicalRecord;
        private FollowUpAppointmentResponse followUpAppointment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorHistoryResponse {
        private Long recordId;
        private String recordCode;
        private Long patientId;
        private String patientName;
        private String patientPhone;
        private String diagnosis;
        private String diseaseName;
        private LocalDateTime examinedAt;
        private Integer prescriptionCount;
        private Boolean hasInvoice;
        private String invoiceStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PatientRecordSummaryResponse {
        private Long recordId;
        private String recordCode;
        private String diagnosis;
        private String diseaseName;
        private String doctorName;
        private String doctorTitle;
        private LocalDateTime examinedAt;
        private Integer prescriptionCount;
        private LocalDate followUpDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorPatientResponse {
        private Long patientId;
        private String patientCode;
        private String fullName;
        private String phone;
        private String lastVisitDate;
    }
}
