package com.backend.clinic.Repository;

import com.backend.clinic.Entity.PatientAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientAllergyRepository extends JpaRepository<PatientAllergy, Long> {
    List<PatientAllergy> findByPatient_PatientId(Long patientId);
    List<PatientAllergy> findByPatient_PatientIdOrderByCreatedAtDesc(Long patientId);
    void deleteByPatient_PatientId(Long patientId);
}
