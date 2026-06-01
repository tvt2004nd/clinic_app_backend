package com.backend.clinic.Controller;

import com.backend.clinic.DTO.PatientDTOs;
import com.backend.clinic.Entity.Patient;
import com.backend.clinic.Entity.PatientAllergy;
import com.backend.clinic.Repository.PatientAllergyRepository;
import com.backend.clinic.Repository.PatientRepository;
import com.backend.clinic.Security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PatientController {

    private final PatientRepository patientRepository;
    private final PatientAllergyRepository patientAllergyRepository;

    @GetMapping("/health-profile")
    public ResponseEntity<?> getHealthProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Patient patient = patientRepository.findByUser_UserId(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("Error: Patient record not found for this user."));

        List<PatientAllergy> allergies = patientAllergyRepository.findByPatient_PatientId(patient.getPatientId());
        
        List<PatientDTOs.AllergyDTO> allergyDTOs = allergies.stream().map(a -> PatientDTOs.AllergyDTO.builder()
                .allergyId(a.getAllergyId())
                .allergen(a.getAllergen())
                .reaction(a.getReaction())
                .severity(a.getSeverity())
                .note(a.getNote())
                .build()).collect(Collectors.toList());

        return ResponseEntity.ok(PatientDTOs.HealthProfileResponse.builder()
                .patientCode(patient.getPatientCode())
                .bloodType(patient.getBloodType())
                .medicalHistory(patient.getMedicalHistory())
                .insuranceNumber(patient.getInsuranceNumber())
                .emergencyContact(patient.getEmergencyContact())
                .emergencyPhone(patient.getEmergencyPhone())
                .allergies(allergyDTOs)
                .build());
    }

    @PutMapping("/health-profile")
    @Transactional
    public ResponseEntity<?> updateHealthProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PatientDTOs.HealthProfileRequest request) {

        Patient patient = patientRepository.findByUser_UserId(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("Error: Patient record not found for this user."));

        patient.setBloodType(request.getBloodType());
        patient.setMedicalHistory(request.getMedicalHistory());
        patient.setInsuranceNumber(request.getInsuranceNumber());
        patient.setEmergencyContact(request.getEmergencyContact());
        patient.setEmergencyPhone(request.getEmergencyPhone());
        
        patientRepository.save(patient);

        // Delete existing allergies
        patientAllergyRepository.deleteByPatient_PatientId(patient.getPatientId());
        
        // Save new allergies
        List<PatientAllergy> newAllergies = new ArrayList<>();
        if (request.getAllergies() != null) {
            for (PatientDTOs.AllergyDTO dto : request.getAllergies()) {
                PatientAllergy pa = PatientAllergy.builder()
                        .patient(patient)
                        .allergen(dto.getAllergen())
                        .reaction(dto.getReaction())
                        .severity(dto.getSeverity() != null ? dto.getSeverity() : "MILD")
                        .note(dto.getNote())
                        .build();
                newAllergies.add(pa);
            }
            patientAllergyRepository.saveAll(newAllergies);
        }

        // Prepare response
        List<PatientDTOs.AllergyDTO> allergyDTOs = newAllergies.stream().map(a -> PatientDTOs.AllergyDTO.builder()
                .allergyId(a.getAllergyId())
                .allergen(a.getAllergen())
                .reaction(a.getReaction())
                .severity(a.getSeverity())
                .note(a.getNote())
                .build()).collect(Collectors.toList());

        return ResponseEntity.ok(PatientDTOs.HealthProfileResponse.builder()
                .patientCode(patient.getPatientCode())
                .bloodType(patient.getBloodType())
                .medicalHistory(patient.getMedicalHistory())
                .insuranceNumber(patient.getInsuranceNumber())
                .emergencyContact(patient.getEmergencyContact())
                .emergencyPhone(patient.getEmergencyPhone())
                .allergies(allergyDTOs)
                .build());
    }
}
