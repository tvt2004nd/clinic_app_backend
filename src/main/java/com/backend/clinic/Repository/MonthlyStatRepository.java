package com.backend.clinic.Repository;

import com.backend.clinic.Entity.MonthlyStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonthlyStatRepository extends JpaRepository<MonthlyStat, Long> {
    Optional<MonthlyStat> findByYearAndMonth(Integer year, Integer month);
}
