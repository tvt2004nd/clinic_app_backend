package com.backend.clinic.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "specialties", uniqueConstraints = {
        @UniqueConstraint(columnNames = "specialty_code", name = "uk_specialties_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "specialty_id")
    private Integer specialtyId;

    @Column(name = "specialty_code", nullable = false, length = 30)
    private String specialtyCode;

    @Column(name = "specialty_name", nullable = false, length = 100)
    private String specialtyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;
}
