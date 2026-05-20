package com.backend.clinic.Repository;

import com.backend.clinic.Entity.MedCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedCategoryRepository extends JpaRepository<MedCategory, Integer> {
    Optional<MedCategory> findByCategoryCode(String categoryCode);
}
