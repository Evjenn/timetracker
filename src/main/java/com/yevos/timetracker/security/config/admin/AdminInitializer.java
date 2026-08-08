package com.yevos.timetracker.security.config.admin;

import com.yevos.timetracker.model.entity.Role;
import com.yevos.timetracker.model.entity.UserEntity;
import com.yevos.timetracker.repository.UserRepository;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.security.admin.username}")
    private String adminUsername;
    @Value("${app.security.admin.password}")
    private String adminPassword;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (!userRepository.existsByUsername(adminUsername)) {
            log.info("Admin account not found. Generating default administrator...");

            UserEntity admin = new UserEntity();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setHourlyRate(BigDecimal.valueOf(100.00));
            admin.setEmail("admin@timetracker.local");

            userRepository.save(admin);
            log.info("Default administrator account successfully created!");
        }
    }
}
