package com.saadaoui.smartwarehouse.auth.config;

import com.saadaoui.smartwarehouse.auth.entity.Role;
import com.saadaoui.smartwarehouse.auth.entity.User;
import com.saadaoui.smartwarehouse.auth.repository.RoleRepository;
import com.saadaoui.smartwarehouse.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${application.admin.email:admin@smartwarehouse.com}")
    private String adminEmail;

    @Value("${application.admin.password:Admin@123}")
    private String adminPassword;

    @Value("${application.seed-default-admin:true}")
    private boolean seedDefaultAdmin;

    @Override
    public void run(String... args) {

        createRole("ADMIN");
        createRole("MANAGER");
        createRole("EMPLOYEE");

        if (seedDefaultAdmin) {
            createDefaultAdmin();
        }

    }

    private void createRole(String roleName) {

        if (!roleRepository.existsByName(roleName)) {

            Role role = Role.builder()
                    .name(roleName)
                    .description(roleName + " Role")
                    .build();

            roleRepository.save(role);

        }

    }

    private void createDefaultAdmin() {

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

        User admin = User.builder()
                .firstName("System")
                .lastName("Administrator")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);

    }

}
