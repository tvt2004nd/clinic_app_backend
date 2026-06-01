package com.backend.clinic.Service;

import com.backend.clinic.DTO.ExaminationDTOs;
import com.backend.clinic.Entity.AiDiagnosis;
import com.backend.clinic.Entity.Appointment;
import com.backend.clinic.Entity.ClinicRoom;
import com.backend.clinic.Entity.Disease;
import com.backend.clinic.Entity.Doctor;
import com.backend.clinic.Entity.DoctorSchedule;
import com.backend.clinic.Entity.ExamPhoto;
import com.backend.clinic.Entity.Invoice;
import com.backend.clinic.Entity.MedicalRecord;
import com.backend.clinic.Entity.Medication;
import com.backend.clinic.Entity.Patient;
import com.backend.clinic.Entity.PatientAllergy;
import com.backend.clinic.Entity.PrescriptionItem;
import com.backend.clinic.Repository.AiDiagnosisRepository;
import com.backend.clinic.Repository.AppointmentRepository;
import com.backend.clinic.Repository.DiseaseRepository;
import com.backend.clinic.Repository.DoctorRepository;
import com.backend.clinic.Repository.DoctorScheduleRepository;
import com.backend.clinic.Repository.ExamPhotoRepository;
import com.backend.clinic.Repository.InvoiceRepository;
import com.backend.clinic.Repository.MedicalRecordRepository;
import com.backend.clinic.Repository.MedicationRepository;
import com.backend.clinic.Repository.PatientAllergyRepository;
import com.backend.clinic.Repository.PatientRepository;
import com.backend.clinic.Repository.PrescriptionItemRepository;
import com.backend.clinic.Security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ExaminationService {

    private static final DateTimeFormatter CODE_TIMESTAMP = DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final Random RANDOM = new Random();

    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AiDiagnosisRepository aiDiagnosisRepository;
    private final DiseaseRepository diseaseRepository;
    private final MedicationRepository medicationRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PatientAllergyRepository patientAllergyRepository;
    private final InvoiceRepository invoiceRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final ExamPhotoRepository examPhotoRepository;

    @Transactional(readOnly = true)
    public List<ExaminationDTOs.PatientLookupResponse> searchPatients(String keyword) {
        return patientRepository.searchPatients(normalizeKeyword(keyword)).stream()
                .limit(20)
                .map(this::mapPatientLookup)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExaminationDTOs.AppointmentQueueItemResponse> getAppointmentQueue(CustomUserDetails userDetails,
                                                                                  LocalDate appointmentDate,
                                                                                  String status,
                                                                                  Long doctorId) {
        Long scopedDoctorId = resolveDoctorScope(userDetails, doctorId);
        LocalDate effectiveDate = appointmentDate != null ? appointmentDate : LocalDate.now();

        return appointmentRepository.searchQueue(scopedDoctorId, effectiveDate, normalizeStatus(status)).stream()
                .map(this::mapAppointmentQueueItem)
                .toList();
    }

    public ExaminationDTOs.MedicalRecordDetailResponse intake(CustomUserDetails userDetails,
                                                              ExaminationDTOs.IntakeRequest request) {
        Appointment appointment;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
            assertDoctorAccess(userDetails, appointment.getDoctor());
        } else {
            if (request.getPatientId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient ID is required for walk-in intake");
            }
            Patient patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));
            Doctor doctor = resolveDoctorForWrite(userDetails, request.getDoctorId());

            appointment = Appointment.builder()
                    .appointmentCode(generateCode("AP"))
                    .patient(patient)
                    .doctor(doctor)
                    .appointmentDate(request.getAppointmentDate() != null ? request.getAppointmentDate() : LocalDate.now())
                    .appointmentTime(request.getAppointmentTime() != null ? request.getAppointmentTime() : LocalTime.now().withSecond(0).withNano(0))
                    .reason(trimToNull(request.getReason()))
                    .status("CONFIRMED")
                    .build();
            appointment = appointmentRepository.save(appointment);
        }

        if ("CANCELLED".equals(appointment.getStatus()) || "NO_SHOW".equals(appointment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot intake a cancelled or no-show appointment");
        }

        MedicalRecord medicalRecord = medicalRecordRepository.findByAppointment_AppointmentId(appointment.getAppointmentId())
                .orElse(null);
        if (medicalRecord == null) {
            medicalRecord = medicalRecordRepository.save(MedicalRecord.builder()
                    .recordCode(generateCode("HS"))
                    .appointment(appointment)
                    .patient(appointment.getPatient())
                    .doctor(appointment.getDoctor())
                    .symptoms(defaultSymptoms(appointment.getReason()))
                    .finalDiagnosis("")
                    .build());
        }

        if (!"COMPLETED".equals(appointment.getStatus())) {
            appointment.setStatus("CONFIRMED");
            appointmentRepository.save(appointment);
        }

        return mapMedicalRecordDetail(medicalRecord);
    }

    @Transactional(readOnly = true)
    public ExaminationDTOs.MedicalRecordDetailResponse getMedicalRecord(CustomUserDetails userDetails, Long recordId) {
        MedicalRecord medicalRecord = getMedicalRecordEntity(recordId);
        assertDoctorOrPatientAccess(userDetails, medicalRecord);
        return mapMedicalRecordDetail(medicalRecord);
    }

    public ExaminationDTOs.MedicalRecordDetailResponse updateSymptoms(CustomUserDetails userDetails,
                                                                      Long recordId,
                                                                      ExaminationDTOs.SymptomsUpdateRequest request) {
        MedicalRecord medicalRecord = getMedicalRecordEntity(recordId);
        assertDoctorAccess(userDetails, medicalRecord.getDoctor());
        medicalRecord.setSymptoms(request.getSymptoms().trim());
        return mapMedicalRecordDetail(medicalRecord);
    }

    public ExaminationDTOs.MedicalRecordDetailResponse updateFinalDiagnosis(CustomUserDetails userDetails,
                                                                            Long recordId,
                                                                            ExaminationDTOs.FinalDiagnosisUpdateRequest request) {
        MedicalRecord medicalRecord = getMedicalRecordEntity(recordId);
        assertDoctorAccess(userDetails, medicalRecord.getDoctor());

        medicalRecord.setFinalDiagnosis(request.getFinalDiagnosis().trim());
        medicalRecord.setTreatmentPlan(trimToNull(request.getTreatmentPlan()));

        if (request.getFinalDiseaseId() != null) {
            Disease disease = diseaseRepository.findById(request.getFinalDiseaseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disease not found"));
            medicalRecord.setFinalDisease(disease);
        } else {
            medicalRecord.setFinalDisease(null);
        }

        return mapMedicalRecordDetail(medicalRecord);
    }

    public ExaminationDTOs.MedicalRecordDetailResponse updatePrescription(CustomUserDetails userDetails,
                                                                          Long recordId,
                                                                          ExaminationDTOs.PrescriptionUpdateRequest request) {
        MedicalRecord medicalRecord = getMedicalRecordEntity(recordId);
        assertDoctorAccess(userDetails, medicalRecord.getDoctor());

        List<ExaminationDTOs.PrescriptionItemRequest> requestedItems =
                request.getItems() != null ? request.getItems() : List.of();
        validatePrescriptionItems(requestedItems);

        List<PrescriptionItem> existingItems = prescriptionItemRepository.findByMedicalRecord_RecordId(recordId);
        restoreStock(existingItems);
        prescriptionItemRepository.deleteAll(existingItems);

        List<PrescriptionItem> newItems = new ArrayList<>();
        for (ExaminationDTOs.PrescriptionItemRequest itemRequest : requestedItems) {
            Medication medication = medicationRepository.findById(itemRequest.getMedicationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Medication not found: " + itemRequest.getMedicationId()));

            if (Boolean.FALSE.equals(medication.getIsActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Medication is inactive: " + medication.getMedName());
            }

            int currentStock = medication.getStockQuantity() != null ? medication.getStockQuantity() : 0;
            if (currentStock < itemRequest.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient stock for medication " + medication.getMedName() + ". Available: " + currentStock);
            }

            medication.setStockQuantity(currentStock - itemRequest.getQuantity());

            newItems.add(PrescriptionItem.builder()
                    .medicalRecord(medicalRecord)
                    .medication(medication)
                    .quantity(itemRequest.getQuantity())
                    .dosageInstruction(trimToNull(itemRequest.getDosageInstruction()))
                    .durationDays(itemRequest.getDurationDays())
                    .unitPrice(medication.getPrice() != null ? medication.getPrice() : BigDecimal.ZERO)
                    .build());
        }

        prescriptionItemRepository.saveAll(newItems);
        return mapMedicalRecordDetail(medicalRecord);
    }

    public ExaminationDTOs.FollowUpScheduleResponse scheduleFollowUp(CustomUserDetails userDetails,
                                                                     Long recordId,
                                                                     ExaminationDTOs.FollowUpRequest request) {
        MedicalRecord medicalRecord = getMedicalRecordEntity(recordId);
        assertDoctorAccess(userDetails, medicalRecord.getDoctor());
        validateReadyForCompletion(medicalRecord);

        if (request.getFollowUpDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Follow-up date cannot be in the past");
        }

        medicalRecord.setFollowUpDate(request.getFollowUpDate());

        Appointment followUpAppointment = null;
        if (!Boolean.FALSE.equals(request.getCreateAppointment())) {
            Doctor followUpDoctor = request.getDoctorId() != null
                    ? resolveDoctorForWrite(userDetails, request.getDoctorId())
                    : medicalRecord.getDoctor();

            followUpAppointment = appointmentRepository.save(Appointment.builder()
                    .appointmentCode(generateCode("AP"))
                    .patient(medicalRecord.getPatient())
                    .doctor(followUpDoctor)
                    .appointmentDate(request.getFollowUpDate())
                    .appointmentTime(request.getFollowUpTime() != null
                            ? request.getFollowUpTime()
                            : medicalRecord.getAppointment().getAppointmentTime())
                    .reason(StringUtils.hasText(request.getReason())
                            ? request.getReason().trim()
                            : "Follow-up from record " + medicalRecord.getRecordCode())
                    .status("CONFIRMED")
                    .build());
        }

        Appointment currentAppointment = medicalRecord.getAppointment();
        currentAppointment.setStatus("COMPLETED");
        appointmentRepository.save(currentAppointment);

        return ExaminationDTOs.FollowUpScheduleResponse.builder()
                .medicalRecord(mapMedicalRecordDetail(medicalRecord))
                .followUpAppointment(mapFollowUpAppointment(followUpAppointment))
                .build();
    }

    public ExaminationDTOs.MedicalRecordDetailResponse completeVisit(CustomUserDetails userDetails, Long recordId) {
        MedicalRecord medicalRecord = getMedicalRecordEntity(recordId);
        assertDoctorAccess(userDetails, medicalRecord.getDoctor());
        validateReadyForCompletion(medicalRecord);
        Appointment appointment = medicalRecord.getAppointment();
        appointment.setStatus("COMPLETED");
        appointmentRepository.save(appointment);

        // Release slot: decrease bookedCount on the schedule
        if (appointment.getSchedule() != null) {
            DoctorSchedule schedule = appointment.getSchedule();
            if (schedule.getBookedCount() > 0) {
                schedule.setBookedCount(schedule.getBookedCount() - 1);
            }
            if ("FULL".equals(schedule.getStatus())) {
                schedule.setStatus("AVAILABLE");
            }
            doctorScheduleRepository.save(schedule);
        }

        return mapMedicalRecordDetail(medicalRecord);
    }

    @Transactional(readOnly = true)
    public List<ExaminationDTOs.PatientRecordSummaryResponse> getMyRecords(CustomUserDetails userDetails) {
        Patient patient = patientRepository.findByUser_UserId(userDetails.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Patient profile not found for current user"));

        List<MedicalRecord> records = medicalRecordRepository
                .findByPatient_PatientIdOrderByExaminedAtDesc(patient.getPatientId());

        return records.stream()
                .map(this::mapPatientRecordSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExaminationDTOs.DoctorHistoryResponse> getDoctorHistory(CustomUserDetails userDetails) {
        Doctor doctor = doctorRepository.findByUser_UserId(userDetails.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Current user is not linked to a doctor profile"));

        List<MedicalRecord> records = medicalRecordRepository
                .findByDoctor_DoctorIdOrderByExaminedAtDesc(doctor.getDoctorId());

        return records.stream()
                .filter(r -> "COMPLETED".equals(r.getAppointment().getStatus()))
                .map(r -> {
                    List<PrescriptionItem> items = prescriptionItemRepository
                            .findByMedicalRecord_RecordId(r.getRecordId());
                    var existingInvoice = invoiceRepository.findByMedicalRecord_RecordId(r.getRecordId());

                    String diagnosis = r.getFinalDiagnosis();
                    if ((diagnosis == null || diagnosis.isBlank())
                            && r.getFinalDisease() != null) {
                        diagnosis = r.getFinalDisease().getDiseaseNameVi();
                    }

                    return ExaminationDTOs.DoctorHistoryResponse.builder()
                            .recordId(r.getRecordId())
                            .recordCode(r.getRecordCode())
                            .patientId(r.getPatient().getPatientId())
                            .patientName(r.getPatient().getUser().getFullName())
                            .patientPhone(r.getPatient().getUser().getPhone())
                            .diagnosis(diagnosis)
                            .diseaseName(r.getFinalDisease() != null
                                    ? r.getFinalDisease().getDiseaseNameVi() : null)
                            .examinedAt(r.getExaminedAt())
                            .prescriptionCount(items.size())
                            .hasInvoice(existingInvoice.isPresent())
                            .invoiceStatus(existingInvoice.map(Invoice::getPaymentStatus).orElse(null))
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExaminationDTOs.MedicationLookupResponse> searchMedications(String keyword, boolean activeOnly) {
        return medicationRepository.searchMedications(normalizeKeyword(keyword), activeOnly, Pageable.unpaged()).stream()
                .limit(50)
                .map(this::mapMedicationLookup)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExaminationDTOs.DiseaseLookupResponse> searchDiseases(String keyword) {
        return diseaseRepository.searchDiseases(normalizeKeyword(keyword)).stream()
                .limit(50)
                .map(this::mapDiseaseLookup)
                .toList();
    }

    private MedicalRecord getMedicalRecordEntity(Long recordId) {
        return medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found"));
    }

    private Long resolveDoctorScope(CustomUserDetails userDetails, Long requestedDoctorId) {
        Optional<Doctor> currentDoctor = doctorRepository.findByUser_UserId(userDetails.getUserId());
        boolean isAdmin = hasRole(userDetails, "ADMIN");

        if (isAdmin) {
            return requestedDoctorId;
        }

        Doctor doctor = currentDoctor.orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user is not linked to a doctor profile"));

        if (requestedDoctorId != null && !Objects.equals(requestedDoctorId, doctor.getDoctorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own appointment queue");
        }

        return doctor.getDoctorId();
    }

    private Doctor resolveDoctorForWrite(CustomUserDetails userDetails, Long requestedDoctorId) {
        Optional<Doctor> currentDoctor = doctorRepository.findByUser_UserId(userDetails.getUserId());
        boolean isAdmin = hasRole(userDetails, "ADMIN");

        if (isAdmin && requestedDoctorId != null) {
            return doctorRepository.findById(requestedDoctorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        }

        if (currentDoctor.isPresent()) {
            Doctor doctor = currentDoctor.get();
            if (requestedDoctorId != null && !Objects.equals(requestedDoctorId, doctor.getDoctorId()) && !isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create records for yourself");
            }
            return doctor;
        }

        if (requestedDoctorId != null) {
            return doctorRepository.findById(requestedDoctorId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor context is required");
    }

    private void assertDoctorOrPatientAccess(CustomUserDetails userDetails, MedicalRecord medicalRecord) {
        boolean isAdmin = hasRole(userDetails, "ADMIN");
        if (isAdmin) return;

        // Doctor access
        Doctor targetDoctor = medicalRecord.getDoctor();
        Optional<Doctor> currentDoctor = doctorRepository.findByUser_UserId(userDetails.getUserId());
        if (currentDoctor.isPresent()) {
            if (Objects.equals(currentDoctor.get().getDoctorId(), targetDoctor.getDoctorId())) {
                return;
            }
        }

        // Patient access — allow patient to view their own record
        if (medicalRecord.getPatient() != null
                && medicalRecord.getPatient().getUser() != null) {
            Long patientUserId = medicalRecord.getPatient().getUser().getUserId();
            if (Objects.equals(patientUserId, userDetails.getUserId())) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this medical record");
    }

    private Doctor assertDoctorAccess(CustomUserDetails userDetails, Doctor targetDoctor) {
        boolean isAdmin = hasRole(userDetails, "ADMIN");
        if (isAdmin) {
            return targetDoctor;
        }

        Doctor currentDoctor = doctorRepository.findByUser_UserId(userDetails.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Current user is not linked to a doctor profile"));

        if (!Objects.equals(currentDoctor.getDoctorId(), targetDoctor.getDoctorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this medical record");
        }

        return currentDoctor;
    }

    private boolean hasRole(CustomUserDetails userDetails, String roleCode) {
        String expectedAuthority = "ROLE_" + roleCode;
        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            if (expectedAuthority.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private void restoreStock(List<PrescriptionItem> existingItems) {
        for (PrescriptionItem item : existingItems) {
            Medication medication = item.getMedication();
            int stock = medication.getStockQuantity() != null ? medication.getStockQuantity() : 0;
            medication.setStockQuantity(stock + item.getQuantity());
        }
    }

    private void validatePrescriptionItems(List<ExaminationDTOs.PrescriptionItemRequest> items) {
        Set<Integer> medicationIds = new HashSet<>();
        for (ExaminationDTOs.PrescriptionItemRequest item : items) {
            if (item.getMedicationId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each prescription item requires a valid medication ID and quantity");
            }
            if (!medicationIds.add(item.getMedicationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Duplicate medication in prescription: " + item.getMedicationId());
            }
        }
    }

    private void validateReadyForCompletion(MedicalRecord medicalRecord) {
        if (!StringUtils.hasText(medicalRecord.getSymptoms())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Symptoms must be recorded before completing the visit");
        }
        if (!StringUtils.hasText(medicalRecord.getFinalDiagnosis())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Final diagnosis is required before completing the visit");
        }
    }

    private ExaminationDTOs.AppointmentQueueItemResponse mapAppointmentQueueItem(Appointment appointment) {
        Long medicalRecordId = medicalRecordRepository.findByAppointment_AppointmentId(appointment.getAppointmentId())
                .map(MedicalRecord::getRecordId)
                .orElse(null);
        ClinicRoom clinicRoom = resolveClinicRoom(appointment);

        return ExaminationDTOs.AppointmentQueueItemResponse.builder()
                .appointmentId(appointment.getAppointmentId())
                .appointmentCode(appointment.getAppointmentCode())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .reason(appointment.getReason())
                .patientId(appointment.getPatient().getPatientId())
                .patientCode(appointment.getPatient().getPatientCode())
                .patientName(appointment.getPatient().getUser().getFullName())
                .patientPhone(appointment.getPatient().getUser().getPhone())
                .doctorId(appointment.getDoctor().getDoctorId())
                .doctorName(appointment.getDoctor().getUser().getFullName())
                .clinicRoomId(clinicRoom != null ? clinicRoom.getRoomId() : null)
                .clinicRoomCode(clinicRoom != null ? clinicRoom.getRoomCode() : null)
                .clinicRoomName(clinicRoom != null ? clinicRoom.getRoomName() : null)
                .medicalRecordId(medicalRecordId)
                .build();
    }

    private ExaminationDTOs.PatientLookupResponse mapPatientLookup(Patient patient) {
        return ExaminationDTOs.PatientLookupResponse.builder()
                .patientId(patient.getPatientId())
                .patientCode(patient.getPatientCode())
                .fullName(patient.getUser().getFullName())
                .phone(patient.getUser().getPhone())
                .gender(patient.getUser().getGender())
                .dateOfBirth(patient.getUser().getDateOfBirth())
                .insuranceNumber(patient.getInsuranceNumber())
                .build();
    }

    private ExaminationDTOs.MedicationLookupResponse mapMedicationLookup(Medication medication) {
        return ExaminationDTOs.MedicationLookupResponse.builder()
                .medicationId(medication.getMedicationId())
                .medCode(medication.getMedCode())
                .medName(medication.getMedName())
                .categoryName(medication.getCategory() != null ? medication.getCategory().getCategoryName() : null)
                .activeIngredient(medication.getActiveIngredient())
                .dosageForm(medication.getDosageForm())
                .unit(medication.getUnit())
                .price(medication.getPrice())
                .stockQuantity(medication.getStockQuantity())
                .isActive(medication.getIsActive())
                .medicationType(medication.getMedicationType())
                .build();
    }

    private ExaminationDTOs.DiseaseLookupResponse mapDiseaseLookup(Disease disease) {
        return ExaminationDTOs.DiseaseLookupResponse.builder()
                .diseaseId(disease.getDiseaseId())
                .diseaseCode(disease.getDiseaseCode())
                .diseaseNameVi(disease.getDiseaseNameVi())
                .diseaseNameEn(disease.getDiseaseNameEn())
                .icd10Code(disease.getIcd10Code())
                .severity(disease.getSeverity())
                .isCancer(disease.getIsCancer())
                .build();
    }

    private ExaminationDTOs.MedicalRecordDetailResponse mapMedicalRecordDetail(MedicalRecord medicalRecord) {
        List<AiDiagnosis> aiDiagnoses = aiDiagnosisRepository
                .findByPatient_PatientIdOrderByCreatedAtDesc(medicalRecord.getPatient().getPatientId());
        List<PatientAllergy> allergies = patientAllergyRepository
                .findByPatient_PatientIdOrderByCreatedAtDesc(medicalRecord.getPatient().getPatientId());
        List<PrescriptionItem> prescriptionItems = prescriptionItemRepository
                .findByMedicalRecord_RecordId(medicalRecord.getRecordId());
        List<String> photoUrls = examPhotoRepository
                .findByRecordIdOrderBySortOrderAsc(medicalRecord.getRecordId())
                .stream().map(ExamPhoto::getImageUrl).toList();

        return ExaminationDTOs.MedicalRecordDetailResponse.builder()
                .recordId(medicalRecord.getRecordId())
                .recordCode(medicalRecord.getRecordCode())
                .appointment(mapAppointmentSummary(medicalRecord.getAppointment()))
                .patient(mapPatientSummary(medicalRecord.getPatient()))
                .doctor(mapDoctorSummary(medicalRecord.getDoctor()))
                .symptoms(medicalRecord.getSymptoms())
                .selectedAiDiagnosis(mapAiDiagnosisSummary(medicalRecord.getAiDiagnosis()))
                .aiDiagnoses(aiDiagnoses.stream().map(this::mapAiDiagnosisSummary).toList())
                .finalDiagnosis(medicalRecord.getFinalDiagnosis())
                .finalDisease(mapDiseaseSummary(medicalRecord.getFinalDisease()))
                .treatmentPlan(medicalRecord.getTreatmentPlan())
                .followUpDate(medicalRecord.getFollowUpDate())
                .allergies(allergies.stream().map(this::mapAllergySummary).toList())
                .prescriptionItems(prescriptionItems.stream().map(this::mapPrescriptionItem).toList())
                .photoUrls(photoUrls)
                .examinedAt(medicalRecord.getExaminedAt())
                .build();
    }

    private ExaminationDTOs.AppointmentSummaryResponse mapAppointmentSummary(Appointment appointment) {
        ClinicRoom clinicRoom = resolveClinicRoom(appointment);

        return ExaminationDTOs.AppointmentSummaryResponse.builder()
                .appointmentId(appointment.getAppointmentId())
                .appointmentCode(appointment.getAppointmentCode())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .reason(appointment.getReason())
                .clinicRoomId(clinicRoom != null ? clinicRoom.getRoomId() : null)
                .clinicRoomCode(clinicRoom != null ? clinicRoom.getRoomCode() : null)
                .clinicRoomName(clinicRoom != null ? clinicRoom.getRoomName() : null)
                .build();
    }

    private ClinicRoom resolveClinicRoom(Appointment appointment) {
        if (appointment.getSchedule() == null) {
            return null;
        }
        return appointment.getSchedule().getClinicRoom();
    }

    private ExaminationDTOs.PatientSummaryResponse mapPatientSummary(Patient patient) {
        return ExaminationDTOs.PatientSummaryResponse.builder()
                .patientId(patient.getPatientId())
                .patientCode(patient.getPatientCode())
                .fullName(patient.getUser().getFullName())
                .phone(patient.getUser().getPhone())
                .gender(patient.getUser().getGender())
                .dateOfBirth(patient.getUser().getDateOfBirth())
                .address(patient.getUser().getAddress())
                .bloodType(patient.getBloodType())
                .insuranceNumber(patient.getInsuranceNumber())
                .medicalHistory(patient.getMedicalHistory())
                .emergencyContact(patient.getEmergencyContact())
                .emergencyPhone(patient.getEmergencyPhone())
                .build();
    }

    private ExaminationDTOs.DoctorSummaryResponse mapDoctorSummary(Doctor doctor) {
        return ExaminationDTOs.DoctorSummaryResponse.builder()
                .doctorId(doctor.getDoctorId())
                .doctorCode(doctor.getDoctorCode())
                .fullName(doctor.getUser().getFullName())
                .title(doctor.getTitle())
                .specialtyName(doctor.getSpecialty() != null ? doctor.getSpecialty().getSpecialtyName() : null)
                .consultationFee(doctor.getConsultationFee())
                .build();
    }

    private ExaminationDTOs.DiseaseSummaryResponse mapDiseaseSummary(Disease disease) {
        if (disease == null) {
            return null;
        }

        return ExaminationDTOs.DiseaseSummaryResponse.builder()
                .diseaseId(disease.getDiseaseId())
                .diseaseCode(disease.getDiseaseCode())
                .diseaseNameVi(disease.getDiseaseNameVi())
                .diseaseNameEn(disease.getDiseaseNameEn())
                .icd10Code(disease.getIcd10Code())
                .severity(disease.getSeverity())
                .isCancer(disease.getIsCancer())
                .build();
    }

    private ExaminationDTOs.AiDiagnosisSummaryResponse mapAiDiagnosisSummary(AiDiagnosis aiDiagnosis) {
        if (aiDiagnosis == null) {
            return null;
        }

        return ExaminationDTOs.AiDiagnosisSummaryResponse.builder()
                .diagnosisId(aiDiagnosis.getDiagnosisId())
                .imagePath(aiDiagnosis.getImagePath())
                .top1Disease(mapDiseaseSummary(aiDiagnosis.getTop1Disease()))
                .top1Confidence(aiDiagnosis.getTop1Confidence())
                .top2Disease(mapDiseaseSummary(aiDiagnosis.getTop2Disease()))
                .top2Confidence(aiDiagnosis.getTop2Confidence())
                .top3Disease(mapDiseaseSummary(aiDiagnosis.getTop3Disease()))
                .top3Confidence(aiDiagnosis.getTop3Confidence())
                .confidenceLevel(aiDiagnosis.getConfidenceLevel())
                .modelConsensus(aiDiagnosis.getModelConsensus())
                .hasCancerWarning(aiDiagnosis.getHasCancerWarning())
                .isConfirmed(aiDiagnosis.getIsConfirmed())
                .confirmedByDoctorId(aiDiagnosis.getConfirmedBy() != null ? aiDiagnosis.getConfirmedBy().getDoctorId() : null)
                .confirmedByDoctorName(aiDiagnosis.getConfirmedBy() != null
                        ? aiDiagnosis.getConfirmedBy().getUser().getFullName()
                        : null)
                .confirmedAt(aiDiagnosis.getConfirmedAt())
                .doctorNote(aiDiagnosis.getDoctorNote())
                .createdAt(aiDiagnosis.getCreatedAt())
                .build();
    }

    private ExaminationDTOs.AllergySummaryResponse mapAllergySummary(PatientAllergy allergy) {
        return ExaminationDTOs.AllergySummaryResponse.builder()
                .allergyId(allergy.getAllergyId())
                .allergen(allergy.getAllergen())
                .reaction(allergy.getReaction())
                .severity(allergy.getSeverity())
                .note(allergy.getNote())
                .build();
    }

    private ExaminationDTOs.PrescriptionItemResponse mapPrescriptionItem(PrescriptionItem item) {
        BigDecimal totalPrice = item.getTotalPrice();
        if (totalPrice == null) {
            totalPrice = (item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO)
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
        }

        return ExaminationDTOs.PrescriptionItemResponse.builder()
                .itemId(item.getItemId())
                .medicationId(item.getMedication().getMedicationId())
                .medCode(item.getMedication().getMedCode())
                .medName(item.getMedication().getMedName())
                .unit(item.getMedication().getUnit())
                .quantity(item.getQuantity())
                .dosageInstruction(item.getDosageInstruction())
                .durationDays(item.getDurationDays())
                .unitPrice(item.getUnitPrice())
                .totalPrice(totalPrice)
                .imageUrl(item.getMedication().getImageUrl())
                .build();
    }

    private ExaminationDTOs.PatientRecordSummaryResponse mapPatientRecordSummary(MedicalRecord record) {
        List<PrescriptionItem> items = prescriptionItemRepository
                .findByMedicalRecord_RecordId(record.getRecordId());

        return ExaminationDTOs.PatientRecordSummaryResponse.builder()
                .recordId(record.getRecordId())
                .recordCode(record.getRecordCode())
                .diagnosis(record.getFinalDiagnosis())
                .diseaseName(record.getFinalDisease() != null
                        ? record.getFinalDisease().getDiseaseNameVi() : null)
                .doctorName(record.getDoctor().getUser().getFullName())
                .doctorTitle(record.getDoctor().getTitle())
                .examinedAt(record.getExaminedAt())
                .prescriptionCount(items.size())
                .followUpDate(record.getFollowUpDate())
                .build();
    }

    private ExaminationDTOs.FollowUpAppointmentResponse mapFollowUpAppointment(Appointment appointment) {
        if (appointment == null) {
            return null;
        }

        return ExaminationDTOs.FollowUpAppointmentResponse.builder()
                .appointmentId(appointment.getAppointmentId())
                .appointmentCode(appointment.getAppointmentCode())
                .appointmentDate(appointment.getAppointmentDate())
                .appointmentTime(appointment.getAppointmentTime())
                .status(appointment.getStatus())
                .doctorId(appointment.getDoctor().getDoctorId())
                .doctorName(appointment.getDoctor().getUser().getFullName())
                .build();
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultSymptoms(String appointmentReason) {
        return StringUtils.hasText(appointmentReason) ? appointmentReason.trim() : "";
    }

    private String generateCode(String prefix) {
        String timestamp = LocalDateTime.now().format(CODE_TIMESTAMP);
        int randomSuffix = 1000 + RANDOM.nextInt(9000);
        return prefix + timestamp + randomSuffix;
    }
}
