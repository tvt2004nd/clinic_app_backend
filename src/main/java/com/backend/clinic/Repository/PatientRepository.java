package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.backend.clinic.Entity.User;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPatientCode(String patientCode);
    Optional<Patient> findByUser_UserId(Long userId);
    Optional<Patient> findByUser(User user);
}
