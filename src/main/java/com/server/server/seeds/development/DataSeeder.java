package com.server.server.seeds.development;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.config.properties.AppProperties;
import com.server.server.enums.user.UserTypeEnum;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.models.payment.Account;
import com.server.server.models.user.User;
import com.server.server.repositories.agency.AgencyBranchRepository;
import com.server.server.repositories.agency.AgencyRepository;
import com.server.server.repositories.payment.AccountRepository;
import com.server.server.repositories.user.UserRepository;
import com.server.server.services.user.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Development seed with one account per role. Enabled by {@code app.seed.enabled}; refuses to
 * run under the {@code prod} profile and only seeds an empty user table. The shared password
 * comes from {@code app.seed.password} or is generated and written to {@value #CREDENTIALS_FILE}.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    static final String CREDENTIALS_FILE = "seed-credentials.local.txt";
    private static final String PRODUCTION_PROFILE = "prod";
    private static final int GENERATED_PASSWORD_BYTES = 12;

    private record SeedUser(String userName, String name, UserTypeEnum role, String mobile, int age,
            boolean primaryAgency, boolean branch, double balance) {
    }

    private static final SeedUser[] USERS = {
            new SeedUser("admin", "System Admin", UserTypeEnum.ADMIN, "+97310000001", 30, false, false, 10000),
            new SeedUser("owner", "Ahmed Owner", UserTypeEnum.OWNER, "+97310000002", 45, true, false, 5000),
            new SeedUser("manager", "Sara Manager", UserTypeEnum.MANAGER, "+97310000003", 35, true, true, 1000),
            new SeedUser("employee", "Ali Employee", UserTypeEnum.EMPLOYEE, "+97310000004", 28, true, true, 500),
            new SeedUser("customer", "John Customer", UserTypeEnum.CUSTOMER, "+97310000005", 25, false, false, 2500),
            new SeedUser("support", "Fatima Support", UserTypeEnum.SUPPORT_AGENT, "+97310000006", 32, false, false, 500),
            new SeedUser("developer", "Dana Developer", UserTypeEnum.DEVELOPER, "+97310000007", 27, false, false, 0),
    };

    private final AppProperties appProperties;
    private final Environment environment;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AgencyRepository agencyRepository;
    private final AgencyBranchRepository branchRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (!appProperties.seed().enabled()) {
            return;
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains(PRODUCTION_PROFILE)) {
            log.warn("Seed is enabled but the prod profile is active. Refusing to seed.");
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Users already exist. Skipping development seed.");
            return;
        }
        String password = resolvePassword();
        Agency agency = createAgency();
        AgencyBranch branch = createBranch(agency);
        for (SeedUser seed : USERS) {
            User user = new User();
            user.setUserName(seed.userName());
            user.setName(seed.name());
            user.setEmail(seed.userName() + "@funtravel.com");
            user.setMobileNumber(seed.mobile());
            user.setAge(seed.age());
            user.setUserType(seed.role());
            user.setAgency(seed.primaryAgency() ? agency : null);
            user.setAgencyBranch(seed.branch() ? branch : null);
            User saved = userService.registerUser(user, password);
            Account wallet = accountRepository.findByUserId(saved.getId()).orElseThrow();
            wallet.setBalance(BigDecimal.valueOf(seed.balance()));
            accountRepository.save(wallet);
            if (seed.role() == UserTypeEnum.OWNER) {
                agency.setAgencyOwner(saved);
                agencyRepository.save(agency);
            }
        }
        log.info("Development seed created {} users (one per role). Emails: <username>@funtravel.com", USERS.length);
    }

    private String resolvePassword() {
        String configured = appProperties.seed().password();
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        byte[] random = new byte[GENERATED_PASSWORD_BYTES];
        new SecureRandom().nextBytes(random);
        String generated = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        try {
            Files.writeString(Path.of(CREDENTIALS_FILE),
                    "Development seed password for every seeded account: " + generated + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            log.info("Generated seed password written to {}", Path.of(CREDENTIALS_FILE).toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write " + CREDENTIALS_FILE, e);
        }
        return generated;
    }

    private Agency createAgency() {
        Agency agency = new Agency();
        agency.setAgencyName("Al Aradi Travel");
        agency.setAddress("Manama, Bahrain");
        agency.setContactNumber("+97312345678");
        agency.setOwnerMobileNumber("+97310000002");
        agency.setActive(true);
        return agencyRepository.save(agency);
    }

    private AgencyBranch createBranch(Agency agency) {
        AgencyBranch branch = new AgencyBranch();
        branch.setBranchName("Main Branch");
        branch.setBranchAddress("Manama, Bahrain");
        branch.setContactNumber("+97312345678");
        branch.setAgency(agency);
        branch.setActive(true);
        return branchRepository.save(branch);
    }
}
