package com.backend.clinic.Controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.clinic.DTO.ClinicManagementDTOs;
import com.backend.clinic.DTO.DoctorDTO;
import com.backend.clinic.DTO.ExaminationDTOs;
import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.MedicalRecord;
import com.backend.clinic.Entity.Specialty;
import com.backend.clinic.Entity.User;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.MedicalRecordRepository;
import com.backend.clinic.Repository.SpecialtyRepository;
import com.backend.clinic.Repository.UserRepository;
import com.backend.clinic.Service.ClinicManagementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    // --- Consolidated Dependencies ---
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicManagementService clinicManagementService;


    // ==========================================
    //               CRUD OPERATIONS
    // ==========================================

    @GetMapping
    public List<DoctorDTO.DoctorResponse> listAll() {
        return doctorRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorDTO.DoctorResponse> getById(@PathVariable Long id) {
        return doctorRepository.findById(id).map(d -> ResponseEntity.ok(toResponse(d))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DoctorDTO.DoctorResponse> create(@RequestBody DoctorDTO.DoctorRequest req) {
        User user = userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
        Specialty sp = specialtyRepository.findById(req.getSpecialtyId()).orElseThrow(() -> new IllegalArgumentException("Specialty not found"));

        Doctor d = Doctor.builder()
                .user(user)
                .doctorCode(req.getDoctorCode())
                .title(req.getTitle())
                .specialty(sp)
                .licenseNumber(req.getLicenseNumber())
                .experienceYears(req.getExperienceYears())
                .consultationFee(req.getConsultationFee())
                .biography(req.getBiography())
                .build();

        Doctor saved = doctorRepository.save(d);
        return ResponseEntity.created(URI.create("/api/doctors/" + saved.getDoctorId())).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoctorDTO.DoctorResponse> update(@PathVariable Long id, @RequestBody DoctorDTO.DoctorRequest req) {
        return doctorRepository.findById(id).map(existing -> {
            if (req.getUserId() != null) existing.setUser(userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found")));
            if (req.getDoctorCode() != null) existing.setDoctorCode(req.getDoctorCode());
            if (req.getTitle() != null) existing.setTitle(req.getTitle());
            if (req.getSpecialtyId() != null) existing.setSpecialty(specialtyRepository.findById(req.getSpecialtyId()).orElseThrow(() -> new IllegalArgumentException("Specialty not found")));
            if (req.getLicenseNumber() != null) existing.setLicenseNumber(req.getLicenseNumber());
            if (req.getExperienceYears() != null) existing.setExperienceYears(req.getExperienceYears());
            if (req.getConsultationFee() != null) existing.setConsultationFee(req.getConsultationFee());
            if (req.getBiography() != null) existing.setBiography(req.getBiography());

            Doctor saved = doctorRepository.save(existing);
            return ResponseEntity.ok(toResponse(saved));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return doctorRepository.findById(id).map(d -> {
            doctorRepository.delete(d);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }


    // ==========================================
    //            BUSINESS LOGIC
    // ==========================================

    @GetMapping("/{doctorId}/schedules")
    public ResponseEntity<?> getDoctorSchedules(@PathVariable Long doctorId) {
        List<ClinicManagementDTOs.DoctorAssignmentResponse> schedules = clinicManagementService
                .searchAssignments(
                        doctorId,
                        null,
                        LocalDate.now(),
                        null,
                        null);
        return ResponseEntity.ok(schedules);
    }

    @PostMapping("/check-schedule-conflict")
    public ResponseEntity<?> checkScheduleConflict(
            @RequestBody @Valid ClinicManagementDTOs.ScheduleConflictCheckRequest request) {
        ClinicManagementDTOs.ScheduleConflictResponse response = clinicManagementService
                .checkDoctorScheduleConflict(
                        request.getDoctorId(),
                        request.getWorkDate(),
                        request.getShiftStart(),
                        request.getShiftEnd(),
                        null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-patients")
    public ResponseEntity<?> getMyPatients(Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Doctor doctor = doctorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Bạn không phải bác sĩ"));

        List<MedicalRecord> records = medicalRecordRepository
                .findByDoctor_DoctorIdOrderByExaminedAtDesc(doctor.getDoctorId());

        Map<Long, ExaminationDTOs.DoctorPatientResponse> patientMap = new LinkedHashMap<>();
        for (MedicalRecord r : records) {
            Long pid = r.getPatient().getPatientId();
            if (!patientMap.containsKey(pid)) {
                patientMap.put(pid, ExaminationDTOs.DoctorPatientResponse.builder()
                        .patientId(pid)
                        .patientCode(r.getPatient().getPatientCode())
                        .fullName(r.getPatient().getUser().getFullName())
                        .phone(r.getPatient().getUser().getPhone())
                        .avatarUrl(r.getPatient().getUser().getAvatarUrl())
                        .lastVisitDate(r.getExaminedAt() != null
                                ? r.getExaminedAt().toLocalDate().toString()
                                : null)
                        .lastRecordId(r.getRecordId())
                        .build());
            }
        }

        return ResponseEntity.ok(patientMap.values());
    }


    // ==========================================
    //            HELPER METHODS
    // ==========================================

    private DoctorDTO.DoctorResponse toResponse(Doctor d) {
        Integer specialtyId = null;
        String specialtyName = null;
        Specialty sp = d.getSpecialty();
        if (sp != null) {
            specialtyId = sp.getSpecialtyId();
            specialtyName = sp.getSpecialtyName();
        }
        return DoctorDTO.DoctorResponse.builder()
                .doctorId(d.getDoctorId())
                .userId(d.getUser() != null ? d.getUser().getUserId() : null)
                .userName(d.getUser() != null ? d.getUser().getUsername() : null)
                .userFullName(d.getUser() != null ? d.getUser().getFullName() : null)
                .doctorCode(d.getDoctorCode())
                .title(d.getTitle())
                .specialtyId(specialtyId)
                .specialtyName(specialtyName)
                .licenseNumber(d.getLicenseNumber())
                .experienceYears(d.getExperienceYears())
                .consultationFee(d.getConsultationFee())
                .biography(d.getBiography())
                .createdAt(d.getCreatedAt())
                .build();
    }
}