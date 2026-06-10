package com.backend.clinic.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.PatientRepository;
import com.backend.clinic.Repository.AppointmentRepository;
import com.backend.clinic.Repository.MedicalRecordRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @GetMapping
    public ResponseEntity<Map<String, Long>> stats() {
        long doctors = doctorRepository.count();
        long patients = patientRepository.count();
        long appointments = appointmentRepository.count();
        long medicalRecords = medicalRecordRepository.count();

        Map<String, Long> body = Map.of(
                "doctors", doctors,
                "patients", patients,
                "appointments", appointments,
                "medicalRecords", medicalRecords
        );

        return ResponseEntity.ok(body);
    }
}
