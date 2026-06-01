package com.backend.clinic.Controller;

import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.DoctorSchedule;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.DoctorScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PublicController {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;

    @Transactional(readOnly = true)
    @GetMapping("/doctors")
    public ResponseEntity<?> getDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        var response = doctors.stream().map(doc -> java.util.Map.<String, Object>of(
                "doctorId", doc.getDoctorId(),
                "fullName", doc.getUser().getFullName(),
                "specialty", doc.getSpecialty().getSpecialtyName(),
                "experienceYears", doc.getExperienceYears(),
                "rating", doc.getRating(),
                "fee", doc.getConsultationFee()
        )).collect(Collectors.toList());
        
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
            boolean isFull = "FULL".equals(s.getStatus()) || s.getBookedCount() > 0;
            return java.util.Map.<String, Object>of(
                    "scheduleId", s.getScheduleId(),
                    "date", s.getWorkDate().toString(),
                    "startTime", s.getShiftStart().toString(),
                    "endTime", s.getShiftEnd().toString(),
                    "status", s.getStatus(),
                    "bookedCount", s.getBookedCount(),
                    "maxPatients", s.getMaxPatients(),
                    "isFull", isFull
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
