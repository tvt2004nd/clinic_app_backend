package com.backend.clinic.Controller;

import com.backend.clinic.DTO.MedicalRecordRequest;
import com.backend.clinic.Entity.*;
import com.backend.clinic.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AiDiagnosisRepository aiDiagnosisRepository;
    private final DiseaseRepository diseaseRepository;
    private final MedicalRecordImageRepository medicalRecordImageRepository;

    @PostMapping
    public ResponseEntity<?> createMedicalRecord(@RequestBody MedicalRecordRequest req) {
        log.info("POST /api/medical-records received: recordCode={}, patientId={}, doctorId={}", req.getRecordCode(), req.getPatientId(), req.getDoctorId());
        try {
            MedicalRecord mr = new MedicalRecord();
            mr.setRecordCode(req.getRecordCode());

            if (req.getAppointmentId() != null) {
                Appointment ap = appointmentRepository.findById(req.getAppointmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
                mr.setAppointment(ap);
            }

            if (req.getPatientId() != null) {
                Patient p = patientRepository.findById(req.getPatientId())
                        .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
                mr.setPatient(p);
            }

            if (req.getDoctorId() != null) {
                Doctor d = doctorRepository.findById(req.getDoctorId())
                        .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
                mr.setDoctor(d);
            }

            mr.setSymptoms(req.getSymptoms());

            if (req.getAiDiagnosisId() != null) {
                AiDiagnosis ai = aiDiagnosisRepository.findById(req.getAiDiagnosisId())
                        .orElse(null);
                mr.setAiDiagnosis(ai);
            }

            mr.setFinalDiagnosis(req.getFinalDiagnosis());

            if (req.getFinalDiseaseId() != null) {
                Disease dis = diseaseRepository.findById(req.getFinalDiseaseId())
                        .orElse(null);
                mr.setFinalDisease(dis);
            }

            mr.setTreatmentPlan(req.getTreatmentPlan());
            mr.setFollowUpDate(req.getFollowUpDate());

            mr.setLesionDescription(req.getLesionDescription());
            mr.setLesionLocations(req.getLesionLocations());
            mr.setLesionFeatures(req.getLesionFeatures());
            mr.setLesionColor(req.getLesionColor());
            mr.setLesionSizeCm(req.getLesionSizeCm());
            mr.setLesionShape(req.getLesionShape());

            mr.setExplainedToPatient(req.getExplainedToPatient() != null && req.getExplainedToPatient());
            mr.setFollowupScheduled(req.getFollowupScheduled() != null && req.getFollowupScheduled());

            MedicalRecord saved = medicalRecordRepository.save(mr);

            // Save images if provided
            List<MedicalRecordImage> savedImages = new ArrayList<>();
            if (req.getImageUrls() != null) {
                for (String url : req.getImageUrls()) {
                    MedicalRecordImage img = MedicalRecordImage.builder()
                            .medicalRecord(saved)
                            .imageUrl(url)
                            .build();
                    savedImages.add(medicalRecordImageRepository.save(img));
                }
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save medical record");
        }
    }
}
