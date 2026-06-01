package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPatientCode(String patientCode);
    Optional<Patient> findByUser_UserId(Long userId);

    @Query("""
            select p from Patient p
            join p.user u
            where (:keyword is null or :keyword = ''
                   or lower(p.patientCode) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
                   or lower(u.email) like lower(concat('%', :keyword, '%')))
            order by u.fullName asc
            """)
    List<Patient> searchPatients(@Param("keyword") String keyword);
}
