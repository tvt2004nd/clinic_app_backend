package com.backend.clinic.Repository;

import com.backend.clinic.Entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    List<DoctorSchedule> findByDoctor_DoctorId(Long doctorId);

    boolean existsByClinicRoom_RoomId(Long roomId);

    @Query("""
            select s from DoctorSchedule s
            join fetch s.doctor d
            join fetch d.user u
            left join fetch d.specialty ds
            left join fetch s.clinicRoom r
            left join fetch r.specialty rs
            where (:doctorId is null or d.doctorId = :doctorId)
              and (:roomId is null or r.roomId = :roomId)
              and (:startDate is null or s.workDate >= :startDate)
              and (:endDate is null or s.workDate <= :endDate)
              and (:status is null or s.status = :status)
            order by s.workDate asc, s.shiftStart asc
            """)
    List<DoctorSchedule> searchAssignments(@Param("doctorId") Long doctorId,
                                           @Param("roomId") Long roomId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("status") String status);

    @Query("""
            select s from DoctorSchedule s
            where s.doctor.doctorId = :doctorId
              and s.workDate = :workDate
              and (:excludedScheduleId is null or s.scheduleId <> :excludedScheduleId)
              and s.status <> 'CANCELLED'
              and s.shiftStart < :shiftEnd
              and s.shiftEnd > :shiftStart
            """)
    List<DoctorSchedule> findDoctorOverlaps(@Param("doctorId") Long doctorId,
                                            @Param("workDate") LocalDate workDate,
                                            @Param("shiftStart") LocalTime shiftStart,
                                            @Param("shiftEnd") LocalTime shiftEnd,
                                            @Param("excludedScheduleId") Long excludedScheduleId);

    @Query("""
            select s from DoctorSchedule s
            where s.clinicRoom.roomId = :roomId
              and s.workDate = :workDate
              and (:excludedScheduleId is null or s.scheduleId <> :excludedScheduleId)
              and s.status <> 'CANCELLED'
              and s.shiftStart < :shiftEnd
              and s.shiftEnd > :shiftStart
            """)
    List<DoctorSchedule> findRoomOverlaps(@Param("roomId") Long roomId,
                                          @Param("workDate") LocalDate workDate,
                                          @Param("shiftStart") LocalTime shiftStart,
                                          @Param("shiftEnd") LocalTime shiftEnd,
                                          @Param("excludedScheduleId") Long excludedScheduleId);
}
