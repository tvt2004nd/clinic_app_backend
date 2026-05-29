package com.backend.clinic.Controller;

import com.backend.clinic.DTO.ExaminationDTOs;
import com.backend.clinic.Security.CustomUserDetails;
import com.backend.clinic.Service.ExaminationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/examinations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('DOCTOR')")
public class ExaminationController {

    private final ExaminationService examinationService;

    @GetMapping("/patients")
    public ResponseEntity<List<ExaminationDTOs.PatientLookupResponse>> searchPatients(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(examinationService.searchPatients(keyword));
    }

    @GetMapping("/appointments")
    public ResponseEntity<List<ExaminationDTOs.AppointmentQueueItemResponse>> getAppointmentQueue(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long doctorId
    ) {
        return ResponseEntity.ok(examinationService.getAppointmentQueue(userDetails, date, status, doctorId));
    }

    @PostMapping("/intake")
    public ResponseEntity<ExaminationDTOs.MedicalRecordDetailResponse> intakePatient(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ExaminationDTOs.IntakeRequest request
    ) {
        return ResponseEntity.ok(examinationService.intake(userDetails, request));
    }

    @GetMapping("/records/{recordId}")
    public ResponseEntity<ExaminationDTOs.MedicalRecordDetailResponse> getMedicalRecord(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId
    ) {
        return ResponseEntity.ok(examinationService.getMedicalRecord(userDetails, recordId));
    }

    @PutMapping("/records/{recordId}/symptoms")
    public ResponseEntity<ExaminationDTOs.MedicalRecordDetailResponse> updateSymptoms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId,
            @Valid @RequestBody ExaminationDTOs.SymptomsUpdateRequest request
    ) {
        return ResponseEntity.ok(examinationService.updateSymptoms(userDetails, recordId, request));
    }

    @PutMapping("/records/{recordId}/ai-reference")
    public ResponseEntity<ExaminationDTOs.MedicalRecordDetailResponse> updateAiReference(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId,
            @RequestBody ExaminationDTOs.AiReferenceUpdateRequest request
    ) {
        return ResponseEntity.ok(examinationService.updateAiReference(userDetails, recordId, request));
    }

    @PutMapping("/records/{recordId}/final-diagnosis")
    public ResponseEntity<ExaminationDTOs.MedicalRecordDetailResponse> updateFinalDiagnosis(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId,
            @Valid @RequestBody ExaminationDTOs.FinalDiagnosisUpdateRequest request
    ) {
        return ResponseEntity.ok(examinationService.updateFinalDiagnosis(userDetails, recordId, request));
    }

    @PutMapping("/records/{recordId}/prescription")
    public ResponseEntity<ExaminationDTOs.MedicalRecordDetailResponse> updatePrescription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId,
            @Valid @RequestBody ExaminationDTOs.PrescriptionUpdateRequest request
    ) {
        return ResponseEntity.ok(examinationService.updatePrescription(userDetails, recordId, request));
    }

    @PostMapping("/records/{recordId}/follow-up")
    public ResponseEntity<ExaminationDTOs.FollowUpScheduleResponse> scheduleFollowUp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId,
            @Valid @RequestBody ExaminationDTOs.FollowUpRequest request
    ) {
        return ResponseEntity.ok(examinationService.scheduleFollowUp(userDetails, recordId, request));
    }

    @PostMapping("/records/{recordId}/complete")
    public ResponseEntity<ExaminationDTOs.MedicalRecordDetailResponse> completeVisit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long recordId
    ) {
        return ResponseEntity.ok(examinationService.completeVisit(userDetails, recordId));
    }

    @GetMapping("/references/medications")
    public ResponseEntity<List<ExaminationDTOs.MedicationLookupResponse>> searchMedications(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(examinationService.searchMedications(keyword, activeOnly));
    }

    @GetMapping("/references/diseases")
    public ResponseEntity<List<ExaminationDTOs.DiseaseLookupResponse>> searchDiseases(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(examinationService.searchDiseases(keyword));
    }
}
