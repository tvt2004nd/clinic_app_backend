package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Disease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiseaseRepository extends JpaRepository<Disease, Integer> {
    Optional<Disease> findByDiseaseCode(String diseaseCode);

    @Query("""
            select d from Disease d
            where (:keyword is null or :keyword = ''
                   or lower(d.diseaseCode) like lower(concat('%', :keyword, '%'))
                   or lower(d.diseaseNameVi) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(d.diseaseNameEn, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(d.icd10Code, '')) like lower(concat('%', :keyword, '%')))
            order by d.diseaseNameVi asc
            """)
    List<Disease> searchDiseases(@Param("keyword") String keyword);
}
