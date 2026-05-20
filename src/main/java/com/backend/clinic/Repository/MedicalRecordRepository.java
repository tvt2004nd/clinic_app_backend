package com.backend.clinic.Repository;

import com.backend.clinic.Entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    Optional<MedicalRecord> findByRecordCode(String recordCode);
    Optional<MedicalRecord> findByAppointment_AppointmentId(Long appointmentId);
}
