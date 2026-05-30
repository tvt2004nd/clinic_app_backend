package com.backend.clinic.Controller;

import com.backend.clinic.Entity.*;
import com.backend.clinic.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final RoleRepository roleRepository;
    private final SpecialtyRepository specialtyRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users;
        if (keyword != null && !keyword.isBlank()) {
            var userList = userRepository.searchUsers(keyword);
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), userList.size());
            var subList = start >= userList.size() ? java.util.Collections.<User>emptyList() : userList.subList(start, end);
            users = new org.springframework.data.domain.PageImpl<>(subList, pageable, userList.size());
        } else {
            users = userRepository.findAll(pageable);
        }
        var result = users.map(u -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("userId", u.getUserId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("fullName", u.getFullName());
            m.put("phone", u.getPhone());
            m.put("gender", u.getGender());
            m.put("isActive", u.getIsActive());
            m.put("createdAt", u.getCreatedAt());
            if (u.getRole() != null) {
                m.put("roleId", u.getRole().getRoleId());
                m.put("roleCode", u.getRole().getRoleCode());
                m.put("roleName", u.getRole().getRoleName());
            }
            return m;
        });
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("userId", u.getUserId());
                    m.put("username", u.getUsername());
                    m.put("email", u.getEmail());
                    m.put("fullName", u.getFullName());
                    m.put("phone", u.getPhone());
                    m.put("gender", u.getGender());
                    m.put("isActive", u.getIsActive());
                    m.put("createdAt", u.getCreatedAt());
                    if (u.getRole() != null) {
                        m.put("roleId", u.getRole().getRoleId());
                        m.put("roleCode", u.getRole().getRoleCode());
                        m.put("roleName", u.getRole().getRoleName());
                    }
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String email = (String) body.get("email");
        String password = (String) body.get("password");
        String fullName = (String) body.get("fullName");
        String phone = (String) body.get("phone");
        String roleCode = (String) body.get("roleCode");

        if (username == null || password == null || fullName == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu thông tin bắt buộc"));
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username đã tồn tại"));
        }
        if (email != null && userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email đã tồn tại"));
        }

        Role role = roleCode != null ? roleRepository.findByRoleCode(roleCode).orElse(null) : null;
        if (role == null) {
            role = roleRepository.findByRoleCode("PATIENT").orElse(null);
        }

        User user = User.builder()
                .username(username)
                .email(email != null ? email : username + "@clinic.com")
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .phone(phone)
                .role(role)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        if ("DOCTOR".equals(role.getRoleCode())) {
            String doctorCode = "DOC" + String.format("%03d", doctorRepository.count() + 1);
            Specialty spec = specialtyRepository.findAll().stream().findFirst().orElse(null);
            Doctor doctor = Doctor.builder()
                    .user(user)
                    .doctorCode(doctorCode)
                    .specialty(spec)
                    .licenseNumber("CCHN-" + doctorCode)
                    .build();
            doctorRepository.save(doctor);
        } else if ("PATIENT".equals(role.getRoleCode())) {
            String patientCode = "BN" + (100000 + (int) (Math.random() * 900000));
            Patient patient = Patient.builder()
                    .user(user)
                    .patientCode(patientCode)
                    .build();
            patientRepository.save(patient);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        String email = (String) body.get("email");
        String fullName = (String) body.get("fullName");
        String phone = (String) body.get("phone");

        if (email != null) user.setEmail(email);
        if (fullName != null) user.setFullName(fullName);
        if (phone != null) user.setPhone(phone);

        // Support both roleId and roleCode for compatibility
        if (body.containsKey("roleId") && body.get("roleId") != null) {
            Integer roleId = ((Number) body.get("roleId")).intValue();
            roleRepository.findById(roleId).ifPresent(user::setRole);
        } else if (body.containsKey("roleCode") && body.get("roleCode") != null) {
            String roleCode = (String) body.get("roleCode");
            roleRepository.findByRoleCode(roleCode).ifPresent(user::setRole);
        }

        if (body.containsKey("isActive") && body.get("isActive") != null) {
            user.setIsActive(Boolean.TRUE.equals(body.get("isActive")));
        }

        userRepository.save(user);

        var result = new LinkedHashMap<String, Object>();
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("fullName", user.getFullName());
        result.put("phone", user.getPhone());
        result.put("isActive", user.getIsActive());
        if (user.getRole() != null) {
            result.put("roleId", user.getRole().getRoleId());
            result.put("roleCode", user.getRole().getRoleCode());
            result.put("roleName", user.getRole().getRoleName());
        }
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("isActive", user.getIsActive()));
    }

    @GetMapping("/doctors")
    public ResponseEntity<?> listDoctors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer specialtyId) {
        var doctors = doctorRepository.searchDoctors(keyword, specialtyId);
        var result = doctors.stream().map(d -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("doctorId", d.getDoctorId());
            m.put("doctorCode", d.getDoctorCode());
            m.put("fullName", d.getUser().getFullName());
            m.put("email", d.getUser().getEmail());
            m.put("phone", d.getUser().getPhone());
            m.put("specialtyId", d.getSpecialty() != null ? d.getSpecialty().getSpecialtyId() : null);
            m.put("specialtyName", d.getSpecialty() != null ? d.getSpecialty().getSpecialtyName() : null);
            m.put("licenseNumber", d.getLicenseNumber());
            m.put("experienceYears", d.getExperienceYears());
            m.put("consultationFee", d.getConsultationFee());
            m.put("rating", d.getRating());
            m.put("totalReviews", d.getTotalReviews());
            m.put("isActive", d.getUser().getIsActive());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/patients")
    public ResponseEntity<?> listPatients(
            @RequestParam(required = false) String keyword) {
        var patients = patientRepository.searchPatients(keyword);
        var result = patients.stream().map(p -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("patientId", p.getPatientId());
            m.put("patientCode", p.getPatientCode());
            m.put("fullName", p.getUser().getFullName());
            m.put("email", p.getUser().getEmail());
            m.put("phone", p.getUser().getPhone());
            m.put("gender", p.getUser().getGender());
            m.put("dateOfBirth", p.getUser().getDateOfBirth());
            m.put("address", p.getUser().getAddress());
            m.put("bloodType", p.getBloodType());
            m.put("insuranceNumber", p.getInsuranceNumber());
            m.put("emergencyContact", p.getEmergencyContact());
            m.put("emergencyPhone", p.getEmergencyPhone());
            m.put("createdAt", p.getCreatedAt());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<?> getPatient(@PathVariable Long id) {
        return patientRepository.findById(id)
                .map(p -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("patientId", p.getPatientId());
                    m.put("patientCode", p.getPatientCode());
                    m.put("fullName", p.getUser().getFullName());
                    m.put("email", p.getUser().getEmail());
                    m.put("phone", p.getUser().getPhone());
                    m.put("gender", p.getUser().getGender());
                    m.put("dateOfBirth", p.getUser().getDateOfBirth());
                    m.put("address", p.getUser().getAddress());
                    m.put("bloodType", p.getBloodType());
                    m.put("medicalHistory", p.getMedicalHistory());
                    m.put("insuranceNumber", p.getInsuranceNumber());
                    m.put("emergencyContact", p.getEmergencyContact());
                    m.put("emergencyPhone", p.getEmergencyPhone());
                    m.put("createdAt", p.getCreatedAt());
                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/appointments")
    public ResponseEntity<?> listAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String keyword) {
        var all = appointmentRepository.findAll(Sort.by("appointmentDate").descending().and(Sort.by("appointmentTime").descending()));
        var result = all.stream()
                .filter(a -> date == null || a.getAppointmentDate().equals(date))
                .filter(a -> status == null || a.getStatus().equals(status))
                .filter(a -> doctorId == null || a.getDoctor().getDoctorId().equals(doctorId))
                .filter(a -> keyword == null || keyword.isBlank()
                        || a.getAppointmentCode().toLowerCase().contains(keyword.toLowerCase())
                        || a.getPatient().getUser().getFullName().toLowerCase().contains(keyword.toLowerCase())
                        || a.getDoctor().getUser().getFullName().toLowerCase().contains(keyword.toLowerCase()))
                .map(a -> {
                    var m = new LinkedHashMap<String, Object>();
                    m.put("appointmentId", a.getAppointmentId());
                    m.put("appointmentCode", a.getAppointmentCode());
                    m.put("patientId", a.getPatient().getPatientId());
                    m.put("patientName", a.getPatient().getUser().getFullName());
                    m.put("patientCode", a.getPatient().getPatientCode());
                    m.put("doctorId", a.getDoctor().getDoctorId());
                    m.put("doctorName", a.getDoctor().getUser().getFullName());
                    m.put("doctorCode", a.getDoctor().getDoctorCode());
                    m.put("appointmentDate", a.getAppointmentDate().toString());
                    m.put("appointmentTime", a.getAppointmentTime().toString());
                    m.put("reason", a.getReason());
                    m.put("status", a.getStatus());
                    m.put("cancelReason", a.getCancelReason());
                    m.put("createdAt", a.getCreatedAt().toString());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    // ────────────────────────────── Patients CRUD ──────────────────────────────

    @PostMapping("/patients")
    public ResponseEntity<?> createPatient(@RequestBody Map<String, Object> body) {
        String fullName = (String) body.get("fullName");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String password = (String) body.get("password");

        if (fullName == null || fullName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Họ tên không được để trống"));
        }
        String baseEmail = (email != null && !email.isBlank()) ? email : "bn" + System.currentTimeMillis() + "@clinic.com";

        Role patientRole = roleRepository.findByRoleCode("PATIENT").orElse(null);
        String username = "bn" + (100000 + (int)(Math.random() * 900000));
        while (userRepository.findByUsername(username).isPresent()) {
            username = "bn" + (100000 + (int)(Math.random() * 900000));
        }

        User user = User.builder()
                .username(username)
                .email(baseEmail)
                .passwordHash(passwordEncoder.encode(password != null ? password : "123456"))
                .fullName(fullName)
                .phone(phone)
                .role(patientRole)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        String patientCode = "BN" + (100000 + (int)(Math.random() * 900000));
        Patient patient = Patient.builder()
                .user(user)
                .patientCode(patientCode)
                .bloodType((String) body.get("bloodType"))
                .insuranceNumber((String) body.get("insuranceNumber"))
                .emergencyContact((String) body.get("emergencyContact"))
                .emergencyPhone((String) body.get("emergencyPhone"))
                .medicalHistory((String) body.get("medicalHistory"))
                .build();
        patient = patientRepository.save(patient);

        return ResponseEntity.status(HttpStatus.CREATED).body(patient);
    }

    @PutMapping("/patients/{id}")
    public ResponseEntity<?> updatePatient(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient == null) return ResponseEntity.notFound().build();

        User user = patient.getUser();
        String fullName = (String) body.get("fullName");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        if (fullName != null) user.setFullName(fullName);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        userRepository.save(user);

        if (body.containsKey("bloodType")) patient.setBloodType((String) body.get("bloodType"));
        if (body.containsKey("insuranceNumber")) patient.setInsuranceNumber((String) body.get("insuranceNumber"));
        if (body.containsKey("emergencyContact")) patient.setEmergencyContact((String) body.get("emergencyContact"));
        if (body.containsKey("emergencyPhone")) patient.setEmergencyPhone((String) body.get("emergencyPhone"));
        if (body.containsKey("medicalHistory")) patient.setMedicalHistory((String) body.get("medicalHistory"));
        patientRepository.save(patient);

        return ResponseEntity.ok(patient);
    }

    @DeleteMapping("/patients/{id}")
    public ResponseEntity<?> deletePatient(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient == null) return ResponseEntity.notFound().build();
        User user = patient.getUser();
        patientRepository.delete(patient);
        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Đã xóa bệnh nhân"));
    }

    // ────────────────────────────── Doctors CRUD ──────────────────────────────

    @PostMapping("/doctors")
    public ResponseEntity<?> createDoctor(@RequestBody Map<String, Object> body) {
        String fullName = (String) body.get("fullName");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        String password = (String) body.get("password");

        if (fullName == null || fullName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Họ tên không được để trống"));
        }
        String baseEmail = (email != null && !email.isBlank()) ? email : "bs" + System.currentTimeMillis() + "@clinic.com";

        Role doctorRole = roleRepository.findByRoleCode("DOCTOR").orElse(null);
        String username = "bs" + (100000 + (int)(Math.random() * 900000));
        while (userRepository.findByUsername(username).isPresent()) {
            username = "bs" + (100000 + (int)(Math.random() * 900000));
        }

        User user = User.builder()
                .username(username)
                .email(baseEmail)
                .passwordHash(passwordEncoder.encode(password != null ? password : "123456"))
                .fullName(fullName)
                .phone(phone)
                .role(doctorRole)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        String doctorCode = "DOC" + String.format("%03d", doctorRepository.count() + 1);
        Integer specialtyId = body.get("specialtyId") != null ? ((Number) body.get("specialtyId")).intValue() : null;
        Specialty specialty = specialtyId != null ? specialtyRepository.findById(specialtyId).orElse(null) : null;

        Doctor doctor = Doctor.builder()
                .user(user)
                .doctorCode(doctorCode)
                .specialty(specialty)
                .licenseNumber((String) body.get("licenseNumber"))
                .title((String) body.get("title"))
                .biography((String) body.get("biography"))
                .experienceYears(body.get("experienceYears") != null ? ((Number) body.get("experienceYears")).intValue() : 0)
                .consultationFee(body.get("consultationFee") != null ? new java.math.BigDecimal(body.get("consultationFee").toString()) : new java.math.BigDecimal("150000"))
                .build();
        doctor = doctorRepository.save(doctor);

        return ResponseEntity.status(HttpStatus.CREATED).body(doctor);
    }

    @PutMapping("/doctors/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor == null) return ResponseEntity.notFound().build();

        User user = doctor.getUser();
        String fullName = (String) body.get("fullName");
        String email = (String) body.get("email");
        String phone = (String) body.get("phone");
        if (fullName != null) user.setFullName(fullName);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        userRepository.save(user);

        if (body.containsKey("licenseNumber")) doctor.setLicenseNumber((String) body.get("licenseNumber"));
        if (body.containsKey("title")) doctor.setTitle((String) body.get("title"));
        if (body.containsKey("biography")) doctor.setBiography((String) body.get("biography"));
        if (body.containsKey("experienceYears")) doctor.setExperienceYears(((Number) body.get("experienceYears")).intValue());
        if (body.containsKey("consultationFee")) doctor.setConsultationFee(new java.math.BigDecimal(body.get("consultationFee").toString()));
        if (body.containsKey("specialtyId")) {
            Integer specId = ((Number) body.get("specialtyId")).intValue();
            doctor.setSpecialty(specialtyRepository.findById(specId).orElse(null));
        }
        doctorRepository.save(doctor);

        return ResponseEntity.ok(doctor);
    }

    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        Doctor doctor = doctorRepository.findById(id).orElse(null);
        if (doctor == null) return ResponseEntity.notFound().build();
        User user = doctor.getUser();
        doctorRepository.delete(doctor);
        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Đã xóa bác sĩ"));
    }

    // ────────────────────────────── Appointments CRUD ──────────────────────────────

    @PostMapping("/appointments")
    public ResponseEntity<?> createAppointment(@RequestBody Map<String, Object> body) {
        Long patientId = body.get("patientId") != null ? ((Number) body.get("patientId")).longValue() : null;
        Long doctorId = body.get("doctorId") != null ? ((Number) body.get("doctorId")).longValue() : null;
        String appointmentDate = (String) body.get("appointmentDate");
        String appointmentTime = (String) body.get("appointmentTime");

        if (patientId == null || doctorId == null || appointmentDate == null || appointmentTime == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Thiếu thông tin bắt buộc"));
        }

        Patient patient = patientRepository.findById(patientId).orElse(null);
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (patient == null || doctor == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bệnh nhân hoặc bác sĩ không tồn tại"));
        }

        String apptCode = "APT" + System.currentTimeMillis();
        Appointment appointment = Appointment.builder()
                .appointmentCode(apptCode)
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(java.time.LocalDate.parse(appointmentDate))
                .appointmentTime(java.time.LocalTime.parse(appointmentTime))
                .reason((String) body.get("reason"))
                .status("SCHEDULED")
                .build();
        appointment = appointmentRepository.save(appointment);

        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    @PutMapping("/appointments/{id}")
    public ResponseEntity<?> updateAppointment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Appointment appointment = appointmentRepository.findById(id).orElse(null);
        if (appointment == null) return ResponseEntity.notFound().build();

        if (body.containsKey("status")) appointment.setStatus((String) body.get("status"));
        if (body.containsKey("cancelReason")) appointment.setCancelReason((String) body.get("cancelReason"));
        if (body.containsKey("reason")) appointment.setReason((String) body.get("reason"));
        if (body.containsKey("appointmentDate")) appointment.setAppointmentDate(java.time.LocalDate.parse((String) body.get("appointmentDate")));
        if (body.containsKey("appointmentTime")) appointment.setAppointmentTime(java.time.LocalTime.parse((String) body.get("appointmentTime")));
        if (body.containsKey("doctorId")) {
            Doctor doc = doctorRepository.findById(((Number) body.get("doctorId")).longValue()).orElse(null);
            if (doc != null) appointment.setDoctor(doc);
        }
        appointmentRepository.save(appointment);

        return ResponseEntity.ok(appointment);
    }

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<?> deleteAppointment(@PathVariable Long id) {
        if (!appointmentRepository.existsById(id)) return ResponseEntity.notFound().build();
        appointmentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa lịch hẹn"));
    }

    @GetMapping("/specialties")
    public ResponseEntity<?> listSpecialties() {
        var specialties = specialtyRepository.findAll();
        var result = specialties.stream().map(s -> Map.of(
                "specialtyId", s.getSpecialtyId(),
                "specialtyCode", s.getSpecialtyCode(),
                "specialtyName", s.getSpecialtyName()
        )).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/roles")
    public ResponseEntity<?> listRoles() {
        var roles = roleRepository.findAll();
        var result = roles.stream().map(r -> Map.of(
                "roleId", r.getRoleId(),
                "roleCode", r.getRoleCode(),
                "roleName", r.getRoleName()
        )).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/invoices")
    public ResponseEntity<?> listInvoices(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Invoice> invoices;
        if (keyword != null && !keyword.isBlank()) {
            invoices = invoiceRepository.searchInvoices(keyword, pageable);
        } else {
            invoices = invoiceRepository.findAll(pageable);
        }
        var result = invoices.map(inv -> {
            var total = inv.getConsultationFee()
                    .add(inv.getMedicationFee())
                    .add(inv.getOtherFee() != null ? inv.getOtherFee() : BigDecimal.ZERO)
                    .subtract(inv.getDiscount() != null ? inv.getDiscount() : BigDecimal.ZERO);
            if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

            var m = new LinkedHashMap<String, Object>();
            m.put("invoiceId", inv.getInvoiceId());
            m.put("invoiceCode", inv.getInvoiceCode());
            m.put("recordId", inv.getMedicalRecord().getRecordId());
            m.put("patientId", inv.getPatient().getPatientId());
            m.put("patientName", inv.getPatient().getUser().getFullName());
            m.put("patientPhone", inv.getPatient().getUser().getPhone());
            m.put("consultationFee", inv.getConsultationFee());
            m.put("medicationFee", inv.getMedicationFee());
            m.put("otherFee", inv.getOtherFee());
            m.put("discount", inv.getDiscount());
            m.put("totalAmount", total);
            m.put("paymentStatus", inv.getPaymentStatus());
            m.put("createdAt", inv.getCreatedAt());
            if (inv.getMedicalRecord().getDoctor() != null) {
                m.put("doctorName", inv.getMedicalRecord().getDoctor().getUser().getFullName());
            }
            String diagnosis = inv.getMedicalRecord().getFinalDiagnosis();
            if ((diagnosis == null || diagnosis.isBlank()) && inv.getMedicalRecord().getFinalDisease() != null) {
                diagnosis = inv.getMedicalRecord().getFinalDisease().getDiseaseNameVi();
            }
            m.put("diagnosis", diagnosis);
            return m;
        });
        return ResponseEntity.ok(result);
    }
}
