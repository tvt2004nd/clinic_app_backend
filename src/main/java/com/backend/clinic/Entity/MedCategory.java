package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "med_categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = "category_code", name = "uk_med_categories_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_code", nullable = false, length = 30)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(length = 255)
    private String description;
}
