package com.backend.clinic.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DoctorDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorRequest {
        private Long userId;
        private String doctorCode;
        private String title;
        private Integer specialtyId;
        private String licenseNumber;
        private Integer experienceYears;
        private BigDecimal consultationFee;
        private String biography;
    }

    @Data
    @Builder
    public static class DoctorResponse {
        private Long doctorId;
        private Long userId;
        private String userName;
        private String userFullName;
        private String doctorCode;
        private String title;
        private Integer specialtyId;
        private String specialtyName;
        private String licenseNumber;
        private Integer experienceYears;
        private BigDecimal consultationFee;
        private String biography;
        private LocalDateTime createdAt;
    }
}
