package com.backend.clinic;

import com.backend.clinic.Entity.*;
import com.backend.clinic.Repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final ClinicRoomRepository clinicRoomRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           SpecialtyRepository specialtyRepository, ClinicRoomRepository clinicRoomRepository,
                           DoctorRepository doctorRepository, DoctorScheduleRepository doctorScheduleRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.specialtyRepository = specialtyRepository;
        this.clinicRoomRepository = clinicRoomRepository;
        this.doctorRepository = doctorRepository;
        this.doctorScheduleRepository = doctorScheduleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Initialize Roles
        Role adminRole = roleRepository.findAll().stream().filter(r -> "ADMIN".equals(r.getRoleCode())).findFirst().orElseGet(() -> roleRepository.save(Role.builder().roleCode("ADMIN").roleName("Admin").build()));
        Role doctorRole = roleRepository.findAll().stream().filter(r -> "DOCTOR".equals(r.getRoleCode())).findFirst().orElseGet(() -> roleRepository.save(Role.builder().roleCode("DOCTOR").roleName("Doctor").build()));
        Role patientRole = roleRepository.findAll().stream().filter(r -> "PATIENT".equals(r.getRoleCode())).findFirst().orElseGet(() -> roleRepository.save(Role.builder().roleCode("PATIENT").roleName("Patient").build()));

        // Initialize Specialties
        Specialty dalieu = specialtyRepository.findAll().stream().filter(s -> "DL".equals(s.getSpecialtyCode())).findFirst().orElseGet(() -> specialtyRepository.save(Specialty.builder().specialtyCode("DL").specialtyName("Da liễu").build()));
        Specialty noikhoa = specialtyRepository.findAll().stream().filter(s -> "NK".equals(s.getSpecialtyCode())).findFirst().orElseGet(() -> specialtyRepository.save(Specialty.builder().specialtyCode("NK").specialtyName("Nội khoa").build()));

        // Initialize Clinic Rooms
        ClinicRoom room1 = clinicRoomRepository.findAll().stream().filter(r -> "R101".equals(r.getRoomCode())).findFirst().orElseGet(() -> clinicRoomRepository.save(ClinicRoom.builder().roomCode("R101").roomName("Phòng Khám 101").floor(1).specialty(dalieu).build()));
        ClinicRoom room2 = clinicRoomRepository.findAll().stream().filter(r -> "R102".equals(r.getRoomCode())).findFirst().orElseGet(() -> clinicRoomRepository.save(ClinicRoom.builder().roomCode("R102").roomName("Phòng Khám 102").floor(1).specialty(dalieu).build()));
        ClinicRoom room3 = clinicRoomRepository.findAll().stream().filter(r -> "R103".equals(r.getRoomCode())).findFirst().orElseGet(() -> clinicRoomRepository.save(ClinicRoom.builder().roomCode("R103").roomName("Phòng Khám 103").floor(1).specialty(dalieu).build()));

        List<ClinicRoom> rooms = clinicRoomRepository.findAll();

        String[] docNames = {
                "BS. Nguyễn Minh Anh", "BS. Trần Văn B", "BS. Lê Quang Minh", "BS. Phạm Thu Thủy", "BS. Hoàng Duy",
                "BS. Vũ Trọng Phụng", "BS. Đặng Thùy Trâm", "BS. Ngô Tất Tố", "BS. Lý Thường Kiệt", "BS. Trần Hưng Đạo"
        };
        String[] docUsernames = {
                "bsminhanh", "bstranvanb", "bslequangminh", "bsphamthuthuy", "bshoangduy",
                "bsvutrongphung", "bsdangthuytram", "bsngotatto", "bslythuongkiet", "bstranhungdao"
        };
        Specialty[] docSpecialties = {
                dalieu, dalieu, dalieu, noikhoa, noikhoa,
                dalieu, dalieu, noikhoa, dalieu, noikhoa
        };

        for (int i = 0; i < 10; i++) {
            String code = "DOC" + String.format("%03d", i + 1);
            if (doctorRepository.findAll().stream().noneMatch(d -> d.getDoctorCode().equals(code))) {
                User user = User.builder()
                        .username(docUsernames[i])
                        .email(docUsernames[i] + "@dermacare.com")
                        .passwordHash(passwordEncoder.encode("password123"))
                        .fullName(docNames[i])
                        .phone("09" + String.format("%08d", i * 11111111))
                        .role(doctorRole)
                        .isActive(true)
                        .build();
                userRepository.save(user);

                Doctor doctor = Doctor.builder()
                        .user(user)
                        .doctorCode(code)
                        .title("BS. Chuyên Khoa")
                        .specialty(docSpecialties[i])
                        .licenseNumber("CCHN-" + code)
                        .experienceYears(5 + i)
                        .consultationFee(new BigDecimal("150000.00").add(new BigDecimal(i * 10000)))
                        .biography("Bác sĩ chuyên khoa giàu kinh nghiệm.")
                        .rating(new BigDecimal("4." + (5 + (i % 5))))
                        .totalReviews(50 + i * 10)
                        .build();
                doctorRepository.save(doctor);

                // Create schedules for this doctor
                LocalDate today = LocalDate.now();
                ClinicRoom room = rooms.get(i % rooms.size());
                
                List<DoctorSchedule> schedules = new java.util.ArrayList<>();
                schedules.add(createSchedule(doctor, room, today, 8, 12));
                schedules.add(createSchedule(doctor, room, today, 13, 17));
                schedules.add(createSchedule(doctor, room, today.plusDays(1), 8, 12));
                schedules.add(createSchedule(doctor, room, today.plusDays(2), 13, 17));
                schedules.add(createSchedule(doctor, room, today.plusDays(3), 8, 12));
                
                doctorScheduleRepository.saveAll(schedules);
            }
        }
    }

    private DoctorSchedule createSchedule(Doctor doctor, ClinicRoom room, LocalDate date, int startHour, int endHour) {
        return DoctorSchedule.builder()
                .doctor(doctor)
                .clinicRoom(room)
                .workDate(date)
                .shiftStart(LocalTime.of(startHour, 0))
                .shiftEnd(LocalTime.of(endHour, 0))
                .maxPatients(20)
                .bookedCount(0)
                .status("AVAILABLE")
                .build();
    }
}
