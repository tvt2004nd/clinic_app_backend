package com.backend.clinic.Controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.DoctorSchedule;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.DoctorScheduleRepository;
import com.backend.clinic.Service.ClinicManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicController {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ClinicManagementService clinicManagementService;

@Transactional(readOnly = true)
    @GetMapping("/doctors")
    public ResponseEntity<?> getDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        var response = doctors.stream().map(doc -> java.util.Map.<String, Object>of(
                "doctorId", doc.getDoctorId(),
                "fullName", doc.getUser().getFullName(),
                
                // === THÊM DÒNG NÀY ĐỂ BACKEND TRẢ VỀ LINK ẢNH ===
                "avatarUrl", doc.getUser().getAvatarUrl() != null ? doc.getUser().getAvatarUrl() : "",
                
                "specialty", doc.getSpecialty().getSpecialtyName(),
                "experienceYears", doc.getExperienceYears(),
                "rating", doc.getRating(),
                "fee", doc.getConsultationFee())).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/schedules/{doctorId}")
    public ResponseEntity<?> getSchedules(@PathVariable Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow();
        // Only return schedules from today onwards
        LocalDate today = LocalDate.now();
        List<DoctorSchedule> schedules = doctorScheduleRepository.findAll().stream()
                .filter(s -> s.getDoctor().getDoctorId().equals(doctorId) && !s.getWorkDate().isBefore(today))
                .collect(Collectors.toList());

        var response = schedules.stream().map(s -> {
            String derived = clinicManagementService.computeScheduleStatus(s);
            boolean isFull = "FULL".equals(derived);
            return java.util.Map.<String, Object>of(
                    "scheduleId", s.getScheduleId(),
                    "date", s.getWorkDate().toString(),
                    "startTime", s.getShiftStart().toString(),
                    "endTime", s.getShiftEnd().toString(),
                    "status", derived,
                    "bookedCount", s.getBookedCount(),
                    "maxPatients", s.getMaxPatients(),
                    "isFull", isFull);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
