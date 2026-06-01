package com.backend.clinic.Repository;

import com.backend.clinic.Entity.MedicalRecord;
import com.backend.clinic.Entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    Optional<MedicalRecord> findByRecordCode(String recordCode);
    Optional<MedicalRecord> findByAppointment_AppointmentId(Long appointmentId);
    Optional<MedicalRecord> findTop1ByPatientOrderByExaminedAtDesc(Patient patient);
    List<MedicalRecord> findByPatient_PatientIdOrderByExaminedAtDesc(Long patientId);
    List<MedicalRecord> findByDoctor_DoctorIdOrderByExaminedAtDesc(Long doctorId);
}
