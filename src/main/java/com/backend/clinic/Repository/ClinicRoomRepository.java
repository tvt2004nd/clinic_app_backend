package com.backend.clinic.Repository;

import com.backend.clinic.Entity.ClinicRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClinicRoomRepository extends JpaRepository<ClinicRoom, Long> {
    Optional<ClinicRoom> findByRoomCode(String roomCode);

    @Query("""
            select r from ClinicRoom r
            left join r.specialty s
            where (:keyword is null or :keyword = ''
                   or lower(r.roomCode) like lower(concat('%', :keyword, '%'))
                   or lower(r.roomName) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(r.location, '')) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(s.specialtyName, '')) like lower(concat('%', :keyword, '%')))
              and (:status is null or r.status = :status)
            order by r.roomCode asc
            """)
    List<ClinicRoom> searchRooms(@Param("keyword") String keyword, @Param("status") String status);
}
