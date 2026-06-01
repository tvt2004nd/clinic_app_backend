package com.backend.clinic.Repository;

import com.backend.clinic.Entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByAppointmentCode(String appointmentCode);
    boolean existsBySchedule_ScheduleId(Long scheduleId);
    boolean existsByDoctor_DoctorIdAndPatient_PatientIdAndStatus(Long doctorId, Long patientId, String status);
    boolean existsByDoctor_DoctorIdAndPatient_PatientIdAndStatusNotIn(Long doctorId, Long patientId, java.util.List<String> statuses);
    boolean existsByDoctor_DoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
            Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime, java.util.List<String> statuses);
    List<Appointment> findByDoctor_DoctorIdAndStatus(Long doctorId, String status);
    List<Appointment> findByDoctor_DoctorIdAndStatusNotIn(Long doctorId, java.util.List<String> statuses);
    List<Appointment> findByPatient_PatientIdAndStatus(Long patientId, String status);
    List<Appointment> findByPatient_PatientIdAndStatusNotIn(Long patientId, java.util.List<String> statuses);

    @Query("""
            select a from Appointment a
            where (:doctorId is null or a.doctor.doctorId = :doctorId)
              and (:appointmentDate is null or a.appointmentDate = :appointmentDate)
              and (:status is null or a.status = :status)
            order by a.appointmentDate asc, a.appointmentTime asc
            """)
    List<Appointment> searchQueue(@Param("doctorId") Long doctorId,
                                  @Param("appointmentDate") LocalDate appointmentDate,
                                  @Param("status") String status);
}
