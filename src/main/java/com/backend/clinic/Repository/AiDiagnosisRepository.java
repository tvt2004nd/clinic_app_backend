package com.backend.clinic.Repository;

import com.backend.clinic.Entity.AiDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiDiagnosisRepository extends JpaRepository<AiDiagnosis, Long> {
    List<AiDiagnosis> findByPatient_PatientId(Long patientId);
    List<AiDiagnosis> findByPatient_PatientIdOrderByCreatedAtDesc(Long patientId);
}
