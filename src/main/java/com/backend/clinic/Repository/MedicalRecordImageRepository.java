package com.backend.clinic.Repository;

import com.backend.clinic.Entity.MedicalRecordImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordImageRepository extends JpaRepository<MedicalRecordImage, Long> {
    List<MedicalRecordImage> findByMedicalRecord_RecordId(Long recordId);
}
