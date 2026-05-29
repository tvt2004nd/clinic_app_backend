package com.backend.clinic.Controller;

import com.backend.clinic.DTO.ClinicManagementDTOs;
import com.backend.clinic.Service.ClinicManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/doctor-workspace")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('DOCTOR')")
public class ClinicManagementController {

    private final ClinicManagementService clinicManagementService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ClinicManagementDTOs.ClinicRoomResponse>> searchRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(clinicManagementService.searchRooms(keyword, status));
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ClinicManagementDTOs.ClinicRoomResponse> getRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(clinicManagementService.getRoom(roomId));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ClinicManagementDTOs.ClinicRoomResponse> createRoom(
            @Valid @RequestBody ClinicManagementDTOs.RoomUpsertRequest request
    ) {
        return ResponseEntity.ok(clinicManagementService.createRoom(request));
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<ClinicManagementDTOs.ClinicRoomResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody ClinicManagementDTOs.RoomUpsertRequest request
    ) {
        return ResponseEntity.ok(clinicManagementService.updateRoom(roomId, request));
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId) {
        clinicManagementService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<ClinicManagementDTOs.DoctorAssignmentResponse>> searchAssignments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(clinicManagementService.searchAssignments(doctorId, roomId, startDate, endDate, status));
    }

    @PostMapping("/assignments")
    public ResponseEntity<ClinicManagementDTOs.DoctorAssignmentResponse> assignDoctor(
            @Valid @RequestBody ClinicManagementDTOs.DoctorAssignmentRequest request
    ) {
        return ResponseEntity.ok(clinicManagementService.assignDoctor(request));
    }

    @PutMapping("/assignments/{scheduleId}")
    public ResponseEntity<ClinicManagementDTOs.DoctorAssignmentResponse> updateAssignment(
            @PathVariable Long scheduleId,
            @Valid @RequestBody ClinicManagementDTOs.DoctorAssignmentRequest request
    ) {
        return ResponseEntity.ok(clinicManagementService.updateAssignment(scheduleId, request));
    }

    @DeleteMapping("/assignments/{scheduleId}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long scheduleId) {
        clinicManagementService.deleteAssignment(scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/references/doctors")
    public ResponseEntity<List<ClinicManagementDTOs.DoctorOptionResponse>> searchDoctors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer specialtyId
    ) {
        return ResponseEntity.ok(clinicManagementService.searchDoctors(keyword, specialtyId));
    }

    @GetMapping("/references/specialties")
    public ResponseEntity<List<ClinicManagementDTOs.SpecialtyOptionResponse>> searchSpecialties(
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(clinicManagementService.searchSpecialties(keyword));
    }
}
