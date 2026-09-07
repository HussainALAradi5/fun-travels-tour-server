package com.server.server.services;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.server.server.enums.UserTypeEnum;
import com.server.server.models.User;
import com.server.server.models.agency.Agency;
import com.server.server.repositories.UserRepository;
import com.server.server.repositories.agency.AgencyBranchRepository;
import com.server.server.repositories.agency.AgencyRepository;
import com.server.server.services.Account.AccountService;

import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class UserService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private AgencyBranchRepository branchRepository;
    @Autowired
    private ExcelImportService excelImportService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountService accountService;

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail()))
            throw new RuntimeException("Email taken");
        if (userRepository.existsByUserName(user.getUserName()))
            throw new RuntimeException("Username taken");

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        
        // 1. Save the user first so they get an ID
        User savedUser = userRepository.save(user);
        
        // 2. Automatically provision their digital wallet
        accountService.createAccountForUser(savedUser);
        
        // 3. Re-fetch or link so the returned object is complete
        savedUser.setAccount(accountService.getAccountByUserId(savedUser.getId()));
        
        return savedUser;
    }

    public User confirmPasswordReset(String identifier, String baseNumber, String token, String newPassword) {
        // We still check both identifier and baseNumber to keep compatibility with your
        // UserController
        String searchBase = (baseNumber != null && !baseNumber.isEmpty()) ? baseNumber : identifier;

        User user = userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByMobileNumber(identifier))
                .or(() -> userRepository.findByMobileNumber(searchBase))
                .filter(User::isActive)
                .orElseThrow(() -> new RuntimeException("User not found or inactive."));

        if (user.getResetToken() == null || !user.getResetToken().equals(token)) {
            throw new RuntimeException("Invalid or expired reset link.");
        }
        if (user.getResetTokenExpiry() != null && user.getResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Reset link expired.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        return userRepository.save(user);
    }

    // --- 100% FREE EMAIL RESET LOGIC ---
    public String requestPasswordReset(String email, String baseNumber) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .filter(User::isActive)
                .orElseThrow(() -> new RuntimeException("Account with this email was not found."));

        String token = java.util.UUID.randomUUID().toString();

        user.setResetToken(token);
        user.setResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        sendResetEmail(user.getEmail(), user.getName(), token);
        return "A secure password reset link has been sent to your email.";
    }

    private void sendResetEmail(String to, String name, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject("Secure Password Reset - Fun Travel Tour");

            String resetLink = "http://localhost:5173/reset-password?token=" + token + "&email=" + to;

            String htmlMsg = "<div style='font-family: sans-serif; padding: 40px; background-color: #f8fafc;'>"
                    + "<div style='max-width: 600px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0;'>"
                    + "<div style='background: #3182ce; padding: 20px; text-align: center; color: white;'><h2>Fun Travel Tour</h2></div>"
                    + "<div style='padding: 30px;'>"
                    + "<h3>Hi " + name + ",</h3>"
                    + "<p>Ready for your next adventure? Let's get your account back on track. Click the button below to reset your password:</p>"
                    + "<div style='text-align: center; margin: 40px 0;'>"
                    + "<a href='" + resetLink
                    + "' style='background: #3182ce; color: white; padding: 15px 30px; text-decoration: none; border-radius: 8px; font-weight: bold;'>Reset Password</a>"
                    + "</div>"
                    + "<p style='font-size: 12px; color: #718096;'>This link expires in 15 minutes. If you didn't request this, please ignore this email.</p>"
                    + "</div></div></div>";

            helper.setText(htmlMsg, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error sending email: " + e.getMessage());
        }
    }

    public User updateUser(Integer id, User incoming) {
        User existing = getUserById(id);
        BeanUtils.copyProperties(incoming, existing, "id", "password", "userName", "profileImageUrl", "agency",
                "agencyBranch");

        Optional.ofNullable(incoming.getBase64Image())
                .filter(img -> !img.isEmpty())
                .map(img -> fileStorageService.saveBase64Image(img, "user_" + id))
                .ifPresentOrElse(existing::setProfileImageUrl,
                        () -> existing.setProfileImageUrl(incoming.getProfileImageUrl()));

        return userRepository.saveAndFlush(existing);
    }

    public User updatePermissions(Integer id, UserTypeEnum type, Integer branchId) {
        User user = getUserById(id);
        user.setUserType(type);

        branchRepository.findById(Optional.ofNullable(branchId).orElse(-1))
                .ifPresentOrElse(branch -> {
                    user.setAgencyBranch(branch);
                    user.setAgency(branch.getAgency());
                }, () -> {
                    user.setAgencyBranch(null);
                    user.setAgency(null);
                });

        return userRepository.save(user);
    }

    public void softDeleteUser(Integer id) {
        User user = getUserById(id);
        user.setActive(false);
        user.setAgencyBranch(null);
        userRepository.save(user);
    }

    public User login(String identifier, String password) {
        User user = userRepository.findByEmailIgnoreCase(identifier)
                .or(() -> userRepository.findByUserNameIgnoreCase(identifier))
                .or(() -> userRepository.findByMobileNumber(identifier))
                .filter(User::isActive)
                .orElseThrow(() -> new RuntimeException("Invalid credentials or inactive account."));

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new RuntimeException("Wrong password.");
        return user;
    }

    public List<User> getAgencyUsers(Integer agencyId, UserTypeEnum type) {
        if (type == null) {
            return userRepository.findByAgencyIdAndIsActiveTrue(agencyId);
        }
        return userRepository.findByAgencyIdAndUserTypeAndIsActiveTrue(agencyId, type);
    }

    @Transactional
    public List<User> bulkImportEmployees(MultipartFile file, Integer agencyId) {
        Agency agency = agencyRepository.findById(agencyId).orElseThrow(() -> new RuntimeException("Agency required"));

        List<User> users = excelImportService.importFile(file, User::new, (user, data) -> {
            user.setUserName(safeGet(data, 0));
            user.setName(safeGet(data, 1));
            user.setEmail(safeGet(data, 2));
            String pass = safeGet(data, 3);
            user.setPassword(passwordEncoder.encode(pass.isEmpty() ? "Default123!" : pass));
            user.setAge(parseAge(safeGet(data, 4)));
            user.setMobileNumber(safeGet(data, 5));
            String roleStr = safeGet(data, 6).toUpperCase();
            user.setUserType(Arrays.stream(UserTypeEnum.values())
                    .filter(e -> e.name().equals(roleStr))
                    .findFirst().orElse(UserTypeEnum.EMPLOYEE));
            branchRepository.findByBranchNameIgnoreCaseAndAgencyId(safeGet(data, 7), agencyId)
                    .ifPresent(user::setAgencyBranch);
            user.setAgency(agency);
            user.setActive(true);
        });

        List<User> filtered = users.stream()
                .filter(u -> !u.getEmail().isEmpty() && !userRepository.existsByEmail(u.getEmail()))
                .toList();

        return userRepository.saveAll(filtered);
    }

    public User getUserById(Integer id) {
        return userRepository.findById(id).filter(User::isActive)
                .orElseThrow(() -> new RuntimeException("User not found or inactive."));
    }

    private String safeGet(List<String> d, int i) {
        return (d != null && i < d.size() && d.get(i) != null) ? d.get(i).trim() : "";
    }

    private int parseAge(String age) {
        try {
            return Integer.parseInt(age);
        } catch (Exception e) {
            return 0;
        }
    }

    public List<User> getUsersByType(UserTypeEnum type) {
        if (type == null) {
            return userRepository.findByIsActiveTrue();
        }
        return userRepository.findByUserTypeAndIsActiveTrue(type);
    }


    public User getCurrentUser() {
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null; // Public user (not logged in)
        }
        
        return userRepository.findByEmailIgnoreCase(auth.getName())
                .filter(User::isActive)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found or inactive."));
    }
}