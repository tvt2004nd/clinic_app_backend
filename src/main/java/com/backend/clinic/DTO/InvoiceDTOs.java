package com.backend.clinic.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientInvoiceResponse {
        private Long invoiceId;
        private String invoiceCode;
        private String doctorName;
        private String recordDiagnosis;
        private BigDecimal consultationFee;
        private BigDecimal medicationFee;
        private BigDecimal otherFee;
        private BigDecimal discount;
        private BigDecimal totalAmount;
        private String paymentStatus;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorInvoiceResponse {
        private Long invoiceId;
        private String invoiceCode;
        private Long recordId;
        private Long patientId;
        private String patientName;
        private BigDecimal consultationFee;
        private BigDecimal medicationFee;
        private BigDecimal totalAmount;
        private String paymentStatus;
        private String recordDiagnosis;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateInvoiceRequest {
        @NotNull(message = "Record ID is required")
        private Long recordId;

        private BigDecimal consultationFee;
        private BigDecimal otherFee;
        private BigDecimal discount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceDetailResponse {
        private Long invoiceId;
        private String invoiceCode;
        private Long recordId;
        private Long patientId;
        private String patientName;
        private BigDecimal consultationFee;
        private BigDecimal medicationFee;
        private BigDecimal otherFee;
        private BigDecimal discount;
        private BigDecimal totalAmount;
        private String paymentStatus;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequest {
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
        private BigDecimal amount;

        private String paymentMethod;
        private String stripePaymentIntentId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePaymentIntentRequest {
        @NotNull(message = "Invoice ID is required")
        private Long invoiceId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePaymentIntentResponse {
        private String clientSecret;
        private Long invoiceId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private Long paymentId;
        private String paymentCode;
        private BigDecimal amount;
        private String paymentMethod;
        private String status;
        private LocalDateTime paidAt;
    }
}
