package com.backend.clinic.Config;

import com.backend.clinic.Entity.Role;
import com.backend.clinic.Entity.User;
import com.backend.clinic.Repository.RoleRepository;
import com.backend.clinic.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // create ADMIN role if missing
        Role adminRole = roleRepository.findByRoleCode("ADMIN").orElseGet(() -> {
            Role r = Role.builder()
                    .roleCode("ADMIN")
                    .roleName("Administrator")
                    .description("Tài khoản quản trị hệ thống")
                    .build();
            Role saved = roleRepository.save(r);
            log.info("Created role: {}", saved.getRoleCode());
            return saved;
        });

        // create admin user if missing
        String adminUsername = "admin";
        if (!userRepository.existsByUsername(adminUsername)) {
            String rawPassword = "Admin123!"; // default password — change after first login
            User user = User.builder()
                    .username(adminUsername)
                    .email("admin@example.com")
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName("Administrator")
                    .phone("0000000000")
                    .role(adminRole)
                    .isActive(true)
                    .build();
            User saved = userRepository.save(user);
            log.info("Created admin user: {} (password: {})", saved.getUsername(), rawPassword);
        } else {
            log.info("Admin user '{}' already exists, skipping creation.", adminUsername);
        }
        // create DOCTOR role if missing
        Role doctorRole = roleRepository.findByRoleCode("DOCTOR").orElseGet(() -> {
            Role r = Role.builder()
                    .roleCode("DOCTOR")
                    .roleName("Bác sĩ")
                    .description("Tài khoản bác sĩ")
                    .build();
            Role saved = roleRepository.save(r);
            log.info("Created role: {}", saved.getRoleCode());
            return saved;
        });

        // create default doctor user if missing
        String doctorUsername = "doctor";
        if (!userRepository.existsByUsername(doctorUsername)) {
            String rawPassword = "Doctor123!"; // default password — change after first login
            User doctor = User.builder()
                    .username(doctorUsername)
                    .email("doctor@example.com")
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .fullName("Dr. Default")
                    .phone("0123456789")
                    .role(doctorRole)
                    .isActive(true)
                    .build();
            User saved = userRepository.save(doctor);
            log.info("Created doctor user: {} (password: {})", saved.getUsername(), rawPassword);
        } else {
            log.info("Doctor user '{}' already exists, skipping creation.", doctorUsername);
        }
    }
}
