package com.backend.clinic.Controller;

import com.backend.clinic.Entity.*;
import com.backend.clinic.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @PostMapping
    public ResponseEntity<?> bookAppointment(@RequestBody Map<String, Object> body,
                                              Authentication auth) {
        try {
            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Tìm hoặc tạo Patient record cho user này
            Patient patient = patientRepository.findByUser_UserId(user.getUserId())
                    .orElseGet(() -> {
                        long count = patientRepository.count() + 1;
                        return patientRepository.save(Patient.builder()
                                .user(user)
                                .patientCode("PT-" + String.format("%06d", count))
                                .build());
                    });

            Long doctorId = Long.parseLong(body.get("doctorId").toString());
            Long scheduleId = Long.parseLong(body.get("scheduleId").toString());
            String reason = body.getOrDefault("reason", "").toString();
            String patientName = body.getOrDefault("patientName", user.getFullName()).toString();
            String phone = body.getOrDefault("phone", user.getPhone()).toString();

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ"));

            DoctorSchedule schedule = doctorScheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ca khám"));

            if ("FULL".equals(schedule.getStatus()) || "CANCELLED".equals(schedule.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Ca khám này không còn lịch trống"));
            }

            // Check if another patient already booked this time slot
            boolean exists = appointmentRepository.existsByDoctor_DoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                    doctorId, schedule.getWorkDate(), schedule.getShiftStart(),
                    List.of("PENDING", "CONFIRMED"));
            if (exists) {
                return ResponseEntity.badRequest().body(Map.of("message", "Khung giờ này đã có người đặt, vui lòng chọn khung giờ khác"));
            }

            String apptCode = "APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Appointment appointment = Appointment.builder()
                    .appointmentCode(apptCode)
                    .patient(patient)
                    .doctor(doctor)
                    .schedule(schedule)
                    .appointmentDate(schedule.getWorkDate())
                    .appointmentTime(schedule.getShiftStart())
                    .reason(reason)
                    .status("PENDING")
                    .build();

            appointmentRepository.save(appointment);

            // Tăng bookedCount
            schedule.setBookedCount(schedule.getBookedCount() + 1);
            if (schedule.getBookedCount() >= schedule.getMaxPatients()) {
                schedule.setStatus("FULL");
            }
            doctorScheduleRepository.save(schedule);

            return ResponseEntity.ok(Map.<String, Object>of(
                    "appointmentId", appointment.getAppointmentId(),
                    "appointmentCode", apptCode,
                    "status", "PENDING",
                    "message", "Đặt lịch thành công! Vui lòng chờ bác sĩ xác nhận."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    // ── PATIENT: Xem lịch hẹn của mình ─────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<?> getMyAppointments(Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Patient patient = patientRepository.findByUser_UserId(user.getUserId()).orElse(null);

        if (patient == null) return ResponseEntity.ok(List.of());

        List<Appointment> appointments = appointmentRepository.searchQueue(null, null, null)
                .stream()
                .filter(a -> a.getPatient() != null && a.getPatient().getPatientId().equals(patient.getPatientId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(appointments.stream().map(a -> Map.<String, Object>of(
                "appointmentId", a.getAppointmentId(),
                "appointmentCode", a.getAppointmentCode(),
                "doctorName", a.getDoctor().getUser().getFullName(),
                "specialty", a.getDoctor().getSpecialty().getSpecialtyName(),
                "date", a.getAppointmentDate().toString(),
                "time", a.getAppointmentTime().toString(),
                "status", a.getStatus(),
                "reason", a.getReason() != null ? a.getReason() : ""
        )).collect(Collectors.toList()));
    }

    // ── DOCTOR: Xem danh sách lịch hẹn cần duyệt ───────────────────────────
    @GetMapping("/doctor")
    public ResponseEntity<?> getDoctorAppointments(@RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String date,
                                                   Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        Doctor doctor = doctorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Bạn không phải bác sĩ"));

        LocalDate localDate = date != null ? LocalDate.parse(date) : null;
        List<Appointment> appointments = appointmentRepository.searchQueue(
                doctor.getDoctorId(), localDate, status
        );

        return ResponseEntity.ok(appointments.stream()
                .filter(a -> a.getPatient() != null)
                .map(a -> {
                    Long recordId = medicalRecordRepository
                            .findByAppointment_AppointmentId(a.getAppointmentId())
                            .map(MedicalRecord::getRecordId)
                            .orElse(null);
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("appointmentId", a.getAppointmentId());
                    map.put("appointmentCode", a.getAppointmentCode());
                    map.put("patientName", a.getPatient().getUser().getFullName());
                    map.put("patientPhone", a.getPatient().getUser().getPhone() != null
                            ? a.getPatient().getUser().getPhone() : "");
                    map.put("date", a.getAppointmentDate().toString());
                    map.put("time", a.getAppointmentTime().toString());
                    map.put("status", a.getStatus());
                    map.put("reason", a.getReason() != null ? a.getReason() : "");
                    map.put("recordId", recordId);
                    return map;
                }).collect(Collectors.toList()));
    }

    // ── DOCTOR: Xác nhận lịch hẹn ──────────────────────────────────────────
    @PutMapping("/{id}/confirm")
    public ResponseEntity<?> confirmAppointment(@PathVariable Long id, Authentication auth) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if (!"PENDING".equals(appt.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lịch hẹn này không ở trạng thái chờ duyệt"));
        }

        appt.setStatus("CONFIRMED");
        appointmentRepository.save(appt);
        conversationRepository.findByDoctor_DoctorIdAndPatient_PatientId(appt.getDoctor().getDoctorId(), appt.getPatient().getPatientId())
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .doctor(appt.getDoctor())
                        .patient(appt.getPatient())
                        .build()));
        return ResponseEntity.ok(Map.of("message", "Đã xác nhận lịch hẹn", "status", "CONFIRMED"));
    }

    // ── DOCTOR: Hoàn tất lịch hẹn ─────────────────────────────────────────
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeAppointment(@PathVariable Long id) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        appt.setStatus("COMPLETED");
        appointmentRepository.save(appt);

        // Giảm bookedCount, nhả slot
        if (appt.getSchedule() != null) {
            DoctorSchedule schedule = appt.getSchedule();
            if (schedule.getBookedCount() > 0) {
                schedule.setBookedCount(schedule.getBookedCount() - 1);
            }
            if ("FULL".equals(schedule.getStatus())) {
                schedule.setStatus("AVAILABLE");
            }
            doctorScheduleRepository.save(schedule);
        }

        return ResponseEntity.ok(Map.of("message", "Đã hoàn tất lịch hẹn", "status", "COMPLETED"));
    }

    // ── DOCTOR / PATIENT: Hủy lịch hẹn ────────────────────────────────────
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id,
                                                @RequestBody(required = false) Map<String, Object> body) {
        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));

        if ("COMPLETED".equals(appt.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không thể hủy lịch hẹn đã hoàn thành"));
        }

        String reason = body != null ? body.getOrDefault("reason", "").toString() : "";
        appt.setStatus("CANCELLED");
        appt.setCancelReason(reason);
        appointmentRepository.save(appt);

        // Giảm bookedCount của ca khám, mở lại trạng thái AVAILABLE
        if (appt.getSchedule() != null) {
            DoctorSchedule schedule = appt.getSchedule();
            if (schedule.getBookedCount() > 0) {
                schedule.setBookedCount(schedule.getBookedCount() - 1);
            }
            if ("FULL".equals(schedule.getStatus())) {
                schedule.setStatus("AVAILABLE");
            }
            doctorScheduleRepository.save(schedule);
        }

        return ResponseEntity.ok(Map.of("message", "Đã hủy lịch hẹn"));
    }
}
