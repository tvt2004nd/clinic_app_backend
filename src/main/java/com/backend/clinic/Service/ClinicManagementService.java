package com.backend.clinic.Service;

import com.backend.clinic.DTO.ClinicManagementDTOs;
import com.backend.clinic.Entity.ClinicRoom;
import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.DoctorSchedule;
import com.backend.clinic.Entity.Specialty;
import com.backend.clinic.Repository.AppointmentRepository;
import com.backend.clinic.Repository.ClinicRoomRepository;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.DoctorScheduleRepository;
import com.backend.clinic.Repository.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ClinicManagementService {

    private static final Set<String> ROOM_STATUSES = Set.of("ACTIVE", "MAINTENANCE", "INACTIVE");
    private static final Set<String> SCHEDULE_STATUSES = Set.of("AVAILABLE", "FULL", "OFF", "CANCELLED");

    private final ClinicRoomRepository clinicRoomRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorRepository doctorRepository;
    private final SpecialtyRepository specialtyRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<ClinicManagementDTOs.ClinicRoomResponse> searchRooms(String keyword, String status) {
        return clinicRoomRepository
                .searchRooms(normalizeKeyword(keyword), normalizeOptionalStatus(status, ROOM_STATUSES))
                .stream()
                .map(this::mapRoom)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClinicManagementDTOs.ClinicRoomResponse getRoom(Long roomId) {
        return mapRoom(getRoomEntity(roomId));
    }

    public ClinicManagementDTOs.ClinicRoomResponse createRoom(ClinicManagementDTOs.RoomUpsertRequest request) {
        String roomCode = normalizeCode(request.getRoomCode());
        clinicRoomRepository.findByRoomCode(roomCode).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room code already exists");
        });

        ClinicRoom room = ClinicRoom.builder()
                .roomCode(roomCode)
                .roomName(request.getRoomName().trim())
                .location(trimToNull(request.getLocation()))
                .floor(request.getFloor())
                .specialty(resolveSpecialty(request.getSpecialtyId()))
                .capacity(request.getCapacity() != null ? request.getCapacity() : 1)
                .description(trimToNull(request.getDescription()))
                .status(normalizeStatus(request.getStatus(), ROOM_STATUSES, "ACTIVE"))
                .build();

        return mapRoom(clinicRoomRepository.save(room));
    }

    public ClinicManagementDTOs.ClinicRoomResponse updateRoom(Long roomId,
            ClinicManagementDTOs.RoomUpsertRequest request) {
        ClinicRoom room = getRoomEntity(roomId);
        String roomCode = normalizeCode(request.getRoomCode());

        clinicRoomRepository.findByRoomCode(roomCode)
                .filter(existing -> !Objects.equals(existing.getRoomId(), roomId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Room code already exists");
                });

        room.setRoomCode(roomCode);
        room.setRoomName(request.getRoomName().trim());
        room.setLocation(trimToNull(request.getLocation()));
        room.setFloor(request.getFloor());
        room.setSpecialty(resolveSpecialty(request.getSpecialtyId()));
        if (request.getCapacity() != null) {
            room.setCapacity(request.getCapacity());
        }
        room.setDescription(trimToNull(request.getDescription()));
        room.setStatus(normalizeStatus(request.getStatus(), ROOM_STATUSES, room.getStatus()));

        return mapRoom(room);
    }

    public void deleteRoom(Long roomId) {
        ClinicRoom room = getRoomEntity(roomId);
        if (doctorScheduleRepository.existsByClinicRoom_RoomId(roomId)) {
            room.setStatus("INACTIVE");
            return;
        }
        clinicRoomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<ClinicManagementDTOs.DoctorAssignmentResponse> searchAssignments(Long doctorId,
            Long roomId,
            LocalDate startDate,
            LocalDate endDate,
            String status) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }

        return doctorScheduleRepository.searchAssignments(
                doctorId,
                roomId,
                startDate,
                endDate,
                normalizeOptionalStatus(status, SCHEDULE_STATUSES)).stream()
                .map(this::mapAssignment)
                .toList();
    }

    public void validateDoctorScheduleConflict(Long doctorId,
            Long roomId,
            LocalDate workDate,
            LocalTime shiftStart,
            LocalTime shiftEnd,
            Long excludedScheduleId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bác sĩ không tồn tại"));
        ClinicRoom room = null;
        if (roomId != null) {
            room = getRoomEntity(roomId);
        }
        validateNoOverlaps(doctor, room, workDate, shiftStart, shiftEnd, excludedScheduleId);
    }

    @Transactional(readOnly = true)
    public ClinicManagementDTOs.ScheduleConflictResponse checkDoctorScheduleConflict(Long doctorId,
            LocalDate workDate,
            LocalTime shiftStart,
            LocalTime shiftEnd,
            Long excludedScheduleId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bác sĩ không tồn tại"));

        List<DoctorSchedule> overlaps = doctorScheduleRepository.findDoctorOverlaps(
                doctor.getDoctorId(),
                workDate,
                shiftStart,
                shiftEnd,
                excludedScheduleId);

        DoctorSchedule existingSchedule = overlaps.isEmpty() ? null : overlaps.get(0);
        return ClinicManagementDTOs.ScheduleConflictResponse.builder()
                .conflict(existingSchedule != null)
                .existingSchedule(existingSchedule != null ? mapAssignment(existingSchedule) : null)
                .build();
    }

    public ClinicManagementDTOs.DoctorAssignmentResponse assignDoctor(
            ClinicManagementDTOs.DoctorAssignmentRequest request) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setBookedCount(0);
        applyAssignment(schedule, request, null);
        return mapAssignment(doctorScheduleRepository.save(schedule));
    }

    public ClinicManagementDTOs.DoctorAssignmentResponse updateAssignment(Long scheduleId,
            ClinicManagementDTOs.DoctorAssignmentRequest request) {
        DoctorSchedule schedule = doctorScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        applyAssignment(schedule, request, scheduleId);
        return mapAssignment(schedule);
    }

    public void deleteAssignment(Long scheduleId) {
        DoctorSchedule schedule = doctorScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        int bookedCount = schedule.getBookedCount() != null ? schedule.getBookedCount() : 0;
        if (bookedCount > 0 || appointmentRepository.existsBySchedule_ScheduleId(scheduleId)) {
            schedule.setStatus("CANCELLED");
            return;
        }

        doctorScheduleRepository.delete(schedule);
    }

    @Transactional(readOnly = true)
    public List<ClinicManagementDTOs.DoctorOptionResponse> searchDoctors(String keyword, Integer specialtyId) {
        return doctorRepository.searchDoctors(normalizeKeyword(keyword), specialtyId).stream()
                .map(this::mapDoctorOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicManagementDTOs.SpecialtyOptionResponse> searchSpecialties(String keyword) {
        return specialtyRepository.searchSpecialties(normalizeKeyword(keyword)).stream()
                .map(this::mapSpecialtyOption)
                .toList();
    }

    private void applyAssignment(DoctorSchedule schedule,
            ClinicManagementDTOs.DoctorAssignmentRequest request,
            Long excludedScheduleId) {
        validateShift(request.getShiftStart(), request.getShiftEnd());

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        ClinicRoom room = getRoomEntity(request.getRoomId());
        // Do not accept manual status from requests here. Status is derived
        // automatically.
        int maxPatients = request.getMaxPatients() != null ? request.getMaxPatients() : 20;
        int bookedCount = schedule.getBookedCount() != null ? schedule.getBookedCount() : 0;

        if (bookedCount > maxPatients) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Max patients cannot be lower than booked count");
        }
        if (!"ACTIVE".equals(room.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room must be ACTIVE before assigning doctors");
        }

        validateNoOverlaps(doctor, room, request.getWorkDate(), request.getShiftStart(), request.getShiftEnd(),
                excludedScheduleId);

        schedule.setDoctor(doctor);
        schedule.setClinicRoom(room);
        schedule.setWorkDate(request.getWorkDate());
        schedule.setShiftStart(request.getShiftStart());
        schedule.setShiftEnd(request.getShiftEnd());
        schedule.setMaxPatients(maxPatients);
        // derive status based on current data
        schedule.setStatus(computeScheduleStatus(schedule));
    }

    public String computeScheduleStatus(DoctorSchedule schedule) {
        if (schedule == null)
            return null;
        String current = schedule.getStatus();
        if ("CANCELLED".equalsIgnoreCase(current)) {
            return "CANCELLED";
        }
        int booked = schedule.getBookedCount() != null ? schedule.getBookedCount() : 0;
        int max = schedule.getMaxPatients() != null ? schedule.getMaxPatients() : 20;

        // If schedule time is in the past, mark as COMPLETED
        java.time.LocalDate nowDate = java.time.LocalDate.now();
        java.time.LocalTime nowTime = java.time.LocalTime.now();
        if (schedule.getWorkDate() != null && schedule.getShiftEnd() != null) {
            if (schedule.getWorkDate().isBefore(nowDate)
                    || (schedule.getWorkDate().isEqual(nowDate) && nowTime.isAfter(schedule.getShiftEnd()))) {
                return "COMPLETED";
            }
        }

        if (booked >= max)
            return "FULL";
        return "AVAILABLE";
    }

    private void validateNoOverlaps(Doctor doctor,
            ClinicRoom room,
            LocalDate workDate,
            LocalTime shiftStart,
            LocalTime shiftEnd,
            Long excludedScheduleId) {
        if (room != null && !doctorScheduleRepository.findDoctorRoomOverlaps(
                doctor.getDoctorId(),
                room.getRoomId(),
                workDate,
                shiftStart,
                shiftEnd,
                excludedScheduleId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bác sĩ đã được phân công tại phòng khác trong khoảng thời gian này.");
        }

        if (!doctorScheduleRepository.findDoctorOverlaps(
                doctor.getDoctorId(),
                workDate,
                shiftStart,
                shiftEnd,
                excludedScheduleId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Bác sĩ đã có lịch khám trong khoảng thời gian này.");
        }

        if (room != null && !doctorScheduleRepository.findRoomOverlaps(
                room.getRoomId(),
                workDate,
                shiftStart,
                shiftEnd,
                excludedScheduleId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Phòng khám đã có lịch trùng ca trong thời gian này.");
        }
    }

    private void validateShift(LocalTime shiftStart, LocalTime shiftEnd) {
        if (shiftStart == null || shiftEnd == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift start and end are required");
        }
        if (!shiftEnd.isAfter(shiftStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shift end must be after shift start");
        }
    }

    private ClinicRoom getRoomEntity(Long roomId) {
        return clinicRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private Specialty resolveSpecialty(Integer specialtyId) {
        if (specialtyId == null) {
            return null;
        }
        return specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Specialty not found"));
    }

    private ClinicManagementDTOs.ClinicRoomResponse mapRoom(ClinicRoom room) {
        if (room == null) {
            return null;
        }

        Specialty specialty = room.getSpecialty();
        return ClinicManagementDTOs.ClinicRoomResponse.builder()
                .roomId(room.getRoomId())
                .roomCode(room.getRoomCode())
                .roomName(room.getRoomName())
                .location(room.getLocation())
                .floor(room.getFloor())
                .specialtyId(specialty != null ? specialty.getSpecialtyId() : null)
                .specialtyName(specialty != null ? specialty.getSpecialtyName() : null)
                .capacity(room.getCapacity())
                .description(room.getDescription())
                .status(room.getStatus())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private ClinicManagementDTOs.DoctorAssignmentResponse mapAssignment(DoctorSchedule schedule) {
        Doctor doctor = schedule.getDoctor();
        Specialty specialty = doctor.getSpecialty();
        return ClinicManagementDTOs.DoctorAssignmentResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .doctorId(doctor.getDoctorId())
                .doctorCode(doctor.getDoctorCode())
                .doctorName(doctor.getUser().getFullName())
                .doctorTitle(doctor.getTitle())
                .doctorSpecialtyId(specialty != null ? specialty.getSpecialtyId() : null)
                .doctorSpecialtyName(specialty != null ? specialty.getSpecialtyName() : null)
                .room(mapRoom(schedule.getClinicRoom()))
                .workDate(schedule.getWorkDate())
                .shiftStart(schedule.getShiftStart())
                .shiftEnd(schedule.getShiftEnd())
                .maxPatients(schedule.getMaxPatients())
                .bookedCount(schedule.getBookedCount())
                .status(computeScheduleStatus(schedule))
                .createdAt(schedule.getCreatedAt())
                .build();
    }

    private ClinicManagementDTOs.DoctorOptionResponse mapDoctorOption(Doctor doctor) {
        Specialty specialty = doctor.getSpecialty();
        return ClinicManagementDTOs.DoctorOptionResponse.builder()
                .doctorId(doctor.getDoctorId())
                .doctorCode(doctor.getDoctorCode())
                .fullName(doctor.getUser().getFullName())
                .title(doctor.getTitle())
                .specialtyId(specialty != null ? specialty.getSpecialtyId() : null)
                .specialtyName(specialty != null ? specialty.getSpecialtyName() : null)
                .licenseNumber(doctor.getLicenseNumber())
                .build();
    }

    private ClinicManagementDTOs.SpecialtyOptionResponse mapSpecialtyOption(Specialty specialty) {
        return ClinicManagementDTOs.SpecialtyOptionResponse.builder()
                .specialtyId(specialty.getSpecialtyId())
                .specialtyCode(specialty.getSpecialtyCode())
                .specialtyName(specialty.getSpecialtyName())
                .iconUrl(specialty.getIconUrl())
                .build();
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeStatus(String status, Set<String> allowedStatuses, String defaultStatus) {
        String normalized = StringUtils.hasText(status) ? status.trim().toUpperCase() : defaultStatus;
        if (!allowedStatuses.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + normalized);
        }
        return normalized;
    }

    private String normalizeOptionalStatus(String status, Set<String> allowedStatuses) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return normalizeStatus(status, allowedStatuses, null);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
