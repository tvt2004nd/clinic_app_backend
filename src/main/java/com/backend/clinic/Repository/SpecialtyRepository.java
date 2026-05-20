package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {
    Optional<Specialty> findBySpecialtyCode(String specialtyCode);
}
