package com.backend.clinic.Controller;

import com.backend.clinic.DTO.DoctorDTO;
import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.Specialty;
import com.backend.clinic.Entity.User;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.SpecialtyRepository;
import com.backend.clinic.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;

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
