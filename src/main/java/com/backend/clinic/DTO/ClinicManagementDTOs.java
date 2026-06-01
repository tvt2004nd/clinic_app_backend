package com.backend.clinic.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ClinicManagementDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomUpsertRequest {
        @NotBlank(message = "Room code is required")
        @Size(max = 20)
        private String roomCode;

        @NotBlank(message = "Room name is required")
        @Size(max = 100)
        private String roomName;

        @Size(max = 255)
        private String location;

        private Integer floor;
        private Integer specialtyId;

        @Min(value = 1, message = "Capacity must be at least 1")
        private Integer capacity;

        private String description;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorAssignmentRequest {
        @NotNull(message = "Doctor ID is required")
        private Long doctorId;

        @NotNull(message = "Room ID is required")
        private Long roomId;

        @NotNull(message = "Work date is required")
        private LocalDate workDate;

        @NotNull(message = "Shift start is required")
        private LocalTime shiftStart;

        @NotNull(message = "Shift end is required")
        private LocalTime shiftEnd;

        @Min(value = 1, message = "Max patients must be at least 1")
        private Integer maxPatients;

        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClinicRoomResponse {
        private Long roomId;
        private String roomCode;
        private String roomName;
        private String location;
        private Integer floor;
        private Integer specialtyId;
        private String specialtyName;
        private Integer capacity;
        private String description;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorOptionResponse {
        private Long doctorId;
        private String doctorCode;
        private String fullName;
        private String title;
        private Integer specialtyId;
        private String specialtyName;
        private String licenseNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecialtyOptionResponse {
        private Integer specialtyId;
        private String specialtyCode;
        private String specialtyName;
        private String iconUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoctorAssignmentResponse {
        private Long scheduleId;
        private Long doctorId;
        private String doctorCode;
        private String doctorName;
        private String doctorTitle;
        private Integer doctorSpecialtyId;
        private String doctorSpecialtyName;
        private ClinicRoomResponse room;
        private LocalDate workDate;
        private LocalTime shiftStart;
        private LocalTime shiftEnd;
        private Integer maxPatients;
        private Integer bookedCount;
        private String status;
        private LocalDateTime createdAt;
    }
}
