package com.backend.clinic.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class PatientDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthProfileResponse {
        private String patientCode;
        private String bloodType;
        private String medicalHistory;
        private String insuranceNumber;
        private String emergencyContact;
        private String emergencyPhone;
        private List<AllergyDTO> allergies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthProfileRequest {
        private String bloodType;
        private String medicalHistory;
        private String insuranceNumber;
        private String emergencyContact;
        private String emergencyPhone;
        private List<AllergyDTO> allergies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllergyDTO {
        private Long allergyId;
        private String allergen;
        private String reaction;
        private String severity;
        private String note;
    }
}
