package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {
    Optional<Specialty> findBySpecialtyCode(String specialtyCode);

    @Query("""
            select s from Specialty s
            where (:keyword is null or :keyword = ''
                   or lower(s.specialtyCode) like lower(concat('%', :keyword, '%'))
                   or lower(s.specialtyName) like lower(concat('%', :keyword, '%')))
            order by s.specialtyName asc
            """)
    List<Specialty> searchSpecialties(@Param("keyword") String keyword);
}
