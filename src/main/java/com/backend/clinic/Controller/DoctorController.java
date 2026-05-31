package com.backend.clinic.Controller;

import com.backend.clinic.DTO.ExaminationDTOs;
import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.MedicalRecord;
import com.backend.clinic.Entity.User;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.MedicalRecordRepository;
import com.backend.clinic.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final UserRepository userRepository;

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
                        .lastVisitDate(r.getExaminedAt() != null
                                ? r.getExaminedAt().toLocalDate().toString() : null)
                        .build());
            }
        }

        return ResponseEntity.ok(patientMap.values());
    }
}
