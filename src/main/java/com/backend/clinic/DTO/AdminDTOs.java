package com.backend.clinic.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class AdminDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicationRequest {
        private String medCode;

        @NotBlank(message = "Tên thuốc không được để trống")
        private String medName;

        private Integer categoryId;
        private String activeIngredient;
        private String dosageForm;
        private String unit;
        private String imageUrl;

        @NotNull(message = "Giá không được để trống")
        @Min(value = 0, message = "Giá phải >= 0")
        private BigDecimal price;

        @Min(value = 0, message = "Số lượng phải >= 0")
        private Integer stockQuantity;

        private Boolean isActive;

        private String medicationType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedicationResponse {
        private Integer medicationId;
        private String medCode;
        private String medName;
        private Integer categoryId;
        private String categoryName;
        private String activeIngredient;
        private String dosageForm;
        private String unit;
        private String imageUrl;
        private BigDecimal price;
        private Integer stockQuantity;
        private Boolean isActive;
        private String medicationType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryResponse {
        private Integer categoryId;
        private String categoryCode;
        private String categoryName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PagedResponse<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
    }
}
