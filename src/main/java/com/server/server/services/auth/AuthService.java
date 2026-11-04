package com.server.server.services.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.server.server.config.properties.AppProperties;
import com.server.server.constants.BookingRules;
import com.server.server.dto.auth.AuthResponse;
import com.server.server.dto.auth.LoginRequest;
import com.server.server.dto.auth.PasswordResetConfirmRequest;
import com.server.server.dto.auth.RegisterRequest;
import com.server.server.enums.user.UserTypeEnum;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.user.User;
import com.server.server.repositories.user.UserRepository;
import com.server.server.services.user.UserService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.server.server.dto.user.UserResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Same answer whether or not the address exists, so accounts cannot be discovered. */
    public static final String RESET_SENT_MESSAGE =
            "If an active account uses this email, a password reset link has been sent.";
    private static final String RESET_PATH = "/reset-password";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;
    private final AppProperties appProperties;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /** Public sign-up always creates a customer account. */
    @Transactional
    public User register(RegisterRequest request) {
        User user = new User();
        user.setUserName(request.userName().trim());
        user.setName(request.name().trim());
        user.setEmail(request.email().trim());
        user.setMobileNumber(request.mobileNumber().trim());
        user.setAge(Objects.requireNonNullElse(request.age(), 0));
        user.setUserType(UserTypeEnum.CUSTOMER);
        return userService.registerUser(user, request.password());
    }

    /** Accepts email, username, or mobile number; Spring Security verifies the password hash. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, request.password()));
        User user = userRepository.findByLoginIdentifier(identifier)
                .orElseThrow(() -> new WorkflowException("INVALID_CREDENTIALS", "Invalid username or password"));
        return new AuthResponse(jwtService.generateToken(user.getEmail()), UserResponse.from(user));
    }

    @Transactional
    public String requestPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(email.trim()).filter(User::isActive).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(BookingRules.PASSWORD_RESET_MINUTES));
            userRepository.save(user);
            sendResetEmail(user, token);
        });
        return RESET_SENT_MESSAGE;
    }

    @Transactional
    public User confirmPasswordReset(PasswordResetConfirmRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.identifier().trim())
                .filter(User::isActive)
                .orElseThrow(AuthService::invalidResetLink);
        boolean tokenMatches = user.getResetToken() != null && MessageDigest.isEqual(
                user.getResetToken().getBytes(StandardCharsets.UTF_8),
                request.token().getBytes(StandardCharsets.UTF_8));
        boolean expired = user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now());
        if (!tokenMatches || expired) {
            throw invalidResetLink();
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        return userRepository.save(user);
    }

    private static WorkflowException invalidResetLink() {
        return new WorkflowException("INVALID_RESET_LINK", "This reset link is invalid or has expired.");
    }

    private void sendResetEmail(User user, String token) {
        String link = appProperties.clientUrl() + RESET_PATH + "?token=" + token
                + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
        String html = "<div style='font-family: sans-serif; padding: 32px;'>"
                + "<h2>Fun Travel Tour</h2>"
                + "<p>Hi " + HtmlUtils.htmlEscape(Objects.requireNonNullElse(user.getName(), "")) + ",</p>"
                + "<p>Use the button below to reset your password.</p>"
                + "<p><a href='" + HtmlUtils.htmlEscape(link) + "' style='background:#3182ce;color:#fff;"
                + "padding:12px 24px;border-radius:8px;text-decoration:none;'>Reset Password</a></p>"
                + "<p style='font-size:12px;color:#718096;'>This link expires in "
                + BookingRules.PASSWORD_RESET_MINUTES + " minutes. If you did not ask for it, ignore this email.</p>"
                + "</div>";
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(user.getEmail());
            helper.setSubject("Secure Password Reset - Fun Travel Tour");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            // The response stays generic; the failure is only logged.
            log.error("Password reset email could not be sent to user {}: {}", user.getId(), e.getMessage());
        }
    }
}
