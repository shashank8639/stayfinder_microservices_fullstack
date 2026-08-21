package com.stayfinder.auth.seed;

import com.stayfinder.auth.entity.Role;
import com.stayfinder.auth.entity.UserAccount;
import com.stayfinder.auth.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@ConditionalOnProperty(name = "stayfinder.auth.seed-demo-users", havingValue = "true")
public class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserSeeder(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("customer@stayfinder.local", "Customer@123", Role.CUSTOMER);
        seed("admin@stayfinder.local", "Admin@123", Role.ADMIN);
        seed("owner@stayfinder.local", "Owner@123", Role.HOTEL_OWNER);
    }

    private void seed(String email, String rawPassword, Role role) {
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRoles(Set.of(role));
        userAccountRepository.save(user);
        log.info("Seeded demo user email={} role={}", email, role);
    }
}
