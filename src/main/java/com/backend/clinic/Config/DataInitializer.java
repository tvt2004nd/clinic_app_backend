package com.backend.clinic.Config;

import com.backend.clinic.Entity.Role;
import com.backend.clinic.Repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotExists("PATIENT", "Bệnh nhân", "Vai trò bệnh nhân");
    }

    private void createRoleIfNotExists(String code, String name, String desc) {
        roleRepository.findByRoleCode(code).orElseGet(() -> {
            Role r = Role.builder()
                    .roleCode(code)
                    .roleName(name)
                    .description(desc)
                    .build();
            Role saved = roleRepository.save(r);
            log.info("Created role: {}", saved.getRoleCode());
            return saved;
        });
    }
}
