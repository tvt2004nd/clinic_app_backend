package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByDoctorCode(String doctorCode);
    Optional<Doctor> findByUser_UserId(Long userId);

    @Query("""
            select d from Doctor d
            join d.user u
            left join d.specialty s
            where (:specialtyId is null or s.specialtyId = :specialtyId)
              and (:keyword is null or :keyword = ''
                   or lower(d.doctorCode) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(d.licenseNumber, '')) like lower(concat('%', :keyword, '%'))
                   or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(s.specialtyName, '')) like lower(concat('%', :keyword, '%')))
            order by u.fullName asc
            """)
    List<Doctor> searchDoctors(@Param("keyword") String keyword, @Param("specialtyId") Integer specialtyId);
}
