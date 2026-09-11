package com.server.server.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.Account.AccountStatus;
import com.server.server.enums.Account.AccountType;
import com.server.server.enums.UserTypeEnum;
import com.server.server.models.Account;
import com.server.server.models.agency.Agency;
import com.server.server.models.agency.AgencyBranch;
import com.server.server.models.User;
import com.server.server.repositories.AccountRepository;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.agency.AgencyBranchRepository;
import com.server.server.repositories.agency.AgencyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyBranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already has users. Skipping seed.");
            return;
        }

        log.info("Seeding database with test data...");

        String password = passwordEncoder.encode("123456");

        // Create agencies
        Agency agency1 = new Agency();
        agency1.setAgencyName("Al Aradi Travel");
        agency1.setAddress("Manama, Bahrain");
        agency1.setContactNumber("+97312345678");
        agency1.setOwnerMobileNumber("+97334567890");
        agency1.setActive(true);
        agency1 = agencyRepository.save(agency1);

        Agency agency2 = new Agency();
        agency2.setAgencyName("Gulf Tours");
        agency2.setAddress("Dubai, UAE");
        agency2.setContactNumber("+97141234567");
        agency2.setOwnerMobileNumber("+971501234567");
        agency2.setActive(true);
        agency2 = agencyRepository.save(agency2);

        // Create branches
        AgencyBranch branch1 = new AgencyBranch();
        branch1.setBranchName("Main Branch");
        branch1.setBranchAddress("Manama, Bahrain");
        branch1.setContactNumber("+97312345678");
        branch1.setAgency(agency1);
        branch1.setActive(true);
        branch1 = branchRepository.save(branch1);

        AgencyBranch branch2 = new AgencyBranch();
        branch2.setBranchName("Dubai Branch");
        branch2.setBranchAddress("Dubai Marina, UAE");
        branch2.setContactNumber("+97141234567");
        branch2.setAgency(agency2);
        branch2.setActive(true);
        branch2 = branchRepository.save(branch2);

        // Create users
        createUser("admin", "System Admin", "admin@funtravel.com", password, "+97310000001", 30, UserTypeEnum.ADMIN,
                null, null, 10000.00);
        createUser("owner", "Ahmed Owner", "owner@funtravel.com", password, "+97310000002", 45, UserTypeEnum.OWNER,
                agency1, null, 5000.00);
        createUser("manager", "Sara Manager", "manager@funtravel.com", password, "+97310000003", 35,
                UserTypeEnum.MANAGER, agency1, branch1, 1000.00);
        createUser("employee", "Ali Employee", "employee@funtravel.com", password, "+97310000004", 28,
                UserTypeEnum.EMPLOYEE, agency1, branch1, 500.00);
        createUser("customer", "John Customer", "customer@funtravel.com", password, "+97310000005", 25,
                UserTypeEnum.CUSTOMER, null, null, 2500.00);
        createUser("support", "Fatima Support", "support@funtravel.com", password, "+97310000006", 32,
                UserTypeEnum.SUPPORT_AGENT, null, null, 500.00);
        createUser("customer2", "Jane Traveler", "customer2@funtravel.com", password, "+97310000007", 29,
                UserTypeEnum.CUSTOMER, null, null, 3000.00);
        createUser("employee2", "Khalid Staff", "employee2@funtravel.com", password, "+97310000008", 26,
                UserTypeEnum.EMPLOYEE, agency2, branch2, 500.00);

        log.info("Database seeded successfully with 8 test users!");
        log.info("Login credentials: username='admin', password='123456'");
    }

    private void createUser(String userName, String name, String email, String password,
            String mobile, int age, UserTypeEnum type,
            Agency agency, AgencyBranch branch, double balance) {

        if (userRepository.existsByEmail(email)) {
            log.info("User {} already exists. Skipping.", email);
            return;
        }

        User user = new User();
        user.setUserName(userName);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setMobileNumber(mobile);
        user.setAge(age);
        user.setUserType(type);
        user.setActive(true);
        user.setAgency(agency);
        user.setAgencyBranch(branch);
        user = userRepository.save(user);

        // Create wallet
        Account account = new Account();
        account.setUser(user);
        account.setAccountNumber("ACC-" + userName.toUpperCase());
        account.setAccountName(name + "'s Wallet");
        account.setBalance(BigDecimal.valueOf(balance));
        account.setType(AccountType.CUSTOMER_WALLET);
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        log.info("Created user: {} ({}) - Role: {} - Balance: ${}", userName, email, type, balance);
    }
}
