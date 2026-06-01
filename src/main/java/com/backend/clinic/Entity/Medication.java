package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "medications", uniqueConstraints = {
        @UniqueConstraint(columnNames = "med_code", name = "uk_medications_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "medication_id")
    private Integer medicationId;

    @Column(name = "med_code", nullable = false, length = 50)
    private String medCode;

    @Column(name = "med_name", nullable = false, length = 255)
    private String medName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private MedCategory category;

    @Column(name = "active_ingredient", length = 255)
    private String activeIngredient;

    @Column(name = "dosage_form", length = 50)
    private String dosageForm;

    @Column(length = 20)
    private String unit;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "medication_type", length = 20)
    private String medicationType;
}
