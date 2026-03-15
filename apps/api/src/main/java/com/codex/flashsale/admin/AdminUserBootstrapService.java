package com.codex.flashsale.admin;

import com.codex.flashsale.config.ApplicationProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserBootstrapService implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationProperties applicationProperties;

    public AdminUserBootstrapService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            ApplicationProperties applicationProperties
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        ApplicationProperties.SeedUsers seedUsers = applicationProperties.getSecurity().getSeedUsers();
        seedIfMissing(seedUsers.getAdminUsername(), seedUsers.getAdminPassword(), seedUsers.getAdminDisplayName(), AdminRole.ADMIN);
        seedIfMissing(
                seedUsers.getOperatorUsername(),
                seedUsers.getOperatorPassword(),
                seedUsers.getOperatorDisplayName(),
                AdminRole.OPERATOR
        );
    }

    private void seedIfMissing(String username, String password, String displayName, AdminRole role) {
        adminUserRepository.findByUsernameIgnoreCase(username)
                .orElseGet(() -> adminUserRepository.save(AdminUser.create(
                        username,
                        passwordEncoder.encode(password),
                        displayName,
                        role
                )));
    }
}
