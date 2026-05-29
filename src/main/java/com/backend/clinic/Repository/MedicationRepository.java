package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Integer> {
    Optional<Medication> findByMedCode(String medCode);

    @Query("""
            select m from Medication m
            where (:activeOnly = false or m.isActive = true)
              and (:keyword is null or :keyword = ''
                   or lower(m.medCode) like lower(concat('%', :keyword, '%'))
                   or lower(m.medName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(m.activeIngredient, '')) like lower(concat('%', :keyword, '%')))
            order by m.medName asc
            """)
    List<Medication> searchMedications(@Param("keyword") String keyword, @Param("activeOnly") boolean activeOnly);
}
