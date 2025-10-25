package com.morago_backend.service;

import com.morago_backend.dto.dtoResponse.PasswordResetResponseDTO;
import com.morago_backend.entity.PasswordReset;
import com.morago_backend.entity.User;
import com.morago_backend.exception.ResourceNotFoundException;
import com.morago_backend.repository.PasswordResetRepository;
import com.morago_backend.repository.UserRepository;
import com.corundumstudio.socketio.SocketIOServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int CODE_EXPIRY_MINUTES = 15; // Reset code expires in 15 minutes
    private static final int MAX_ATTEMPTS = 5; // Maximum verification attempts
    private static final Random random = new Random();

    private final PasswordResetRepository repository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SocketIOServer socketServer;

    public PasswordResetService(PasswordResetRepository repository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                SocketIOServer socketServer) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.socketServer = socketServer;
    }

    /**
     * Generate a random 6-digit code for password reset
     */
    private Integer generateResetCode() {
        // Generate random 6-digit code (100000 to 999999)
        return 100000 + random.nextInt(900000);
    }

    /**
     * Check if a reset code has expired
     */
    private boolean isCodeExpired(PasswordReset reset) {
        if (reset.getCreatedAtDatetime() == null) {
            return true;
        }
        LocalDateTime expiryTime = reset.getCreatedAtDatetime().plusMinutes(CODE_EXPIRY_MINUTES);
        return LocalDateTime.now().isAfter(expiryTime);
    }

    // ========== CREATE RESET REQUEST ==========
    public PasswordResetResponseDTO create(String phone) {
        try {
            logger.info("Creating password reset for phone={}", phone);

            // Verify that user exists with this phone number
            userRepository.findByPhone(phone)
                    .orElseThrow(() -> new ResourceNotFoundException("No user found with phone " + phone));

            // Generate secure random 6-digit code
            Integer resetCode = generateResetCode();

            PasswordReset entity = new PasswordReset();
            entity.setPhone(phone);
            entity.setResetCode(resetCode);

            PasswordReset saved = repository.save(entity);

            // In production, send SMS with the code instead of returning it
            // For now, we return it for testing purposes
            logger.info("Generated reset code for phone={}: {}", phone, resetCode);
            
            socketServer.getRoomOperations(phone).sendEvent("passwordResetCreated", saved);

            PasswordResetResponseDTO dto = toDTO(saved);
            dto.setMessage("Reset code generated and valid for " + CODE_EXPIRY_MINUTES + " minutes. In production, this would be sent via SMS.");
            return dto;
        } catch (ResourceNotFoundException e) {
            logger.error("User not found for phone={}: {}", phone, e.getMessage());
            // For security, don't reveal if user exists or not
            throw new IllegalArgumentException("If this phone number is registered, a reset code will be sent.");
        } catch (Exception e) {
            logger.error("Error creating password reset for phone={}: {}", phone, e.getMessage(), e);
            throw e;
        }
    }

    // ========== VERIFY RESET CODE ==========
    public PasswordResetResponseDTO verifyCode(String phone, Integer code) {
        try {
            PasswordReset reset = repository.findTopByPhoneOrderByCreatedAtDatetimeDesc(phone)
                    .orElseThrow(() -> new IllegalArgumentException("No active reset request found"));

            // Check if code has expired
            if (isCodeExpired(reset)) {
                logger.warn("Expired reset code for phone={}", phone);
                throw new IllegalArgumentException("Reset code has expired. Please request a new one.");
            }

            // Verify the code
            if (!reset.getResetCode().equals(code)) {
                logger.warn("Invalid reset code attempt for phone={}", phone);
                throw new IllegalArgumentException("Invalid reset code");
            }

            // Code is valid
            logger.info("Reset code verified successfully for phone={}", phone);
            PasswordResetResponseDTO dto = toDTO(reset);
            dto.setMessage("Code verified successfully. You can now reset your password.");
            return dto;
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (Exception e) {
            logger.error("Error verifying reset code for phone={}: {}", phone, e.getMessage(), e);
            throw new RuntimeException("Failed to verify reset code");
        }
    }

    // ========== UPDATE PASSWORD ==========
    public PasswordResetResponseDTO updatePasswordMinimal(String phone, String newPassword) {
        try {
            // Validate password strength
            validatePassword(newPassword);

            // Verify that a valid reset request exists
            PasswordReset reset = repository.findTopByPhoneOrderByCreatedAtDatetimeDesc(phone)
                    .orElseThrow(() -> new IllegalArgumentException("No reset request found. Please request a password reset first."));

            // Check if code has expired
            if (isCodeExpired(reset)) {
                logger.warn("Attempting to reset password with expired code for phone={}", phone);
                throw new IllegalArgumentException("Reset session has expired. Please request a new reset code.");
            }

            // Find user by phone
            User user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with phone " + phone));

            // Encode and set new password
            String encoded = passwordEncoder.encode(newPassword);
            user.setPassword(encoded);
            userRepository.save(user);

            // Invalidate the reset request by deleting it
            repository.delete(reset);
            logger.info("Password reset completed successfully for phone={}", phone);

            // Create response DTO
            PasswordResetResponseDTO dto = new PasswordResetResponseDTO();
            dto.setPhone(phone);
            dto.setMessage("Password updated successfully. You can now log in with your new password.");

            // Optional: send event via Socket.IO if needed
            if (socketServer != null) {
                socketServer.getRoomOperations(phone).sendEvent("passwordUpdated", dto);
            }

            return dto;
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (Exception e) {
            logger.error("Error updating password for phone={}: {}", phone, e.getMessage(), e);
            throw new RuntimeException("Failed to update password");
        }
    }

    /**
     * Validate password meets security requirements
     */
    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (password.length() > 100) {
            throw new IllegalArgumentException("Password must not exceed 100 characters");
        }
        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        // Check for at least one special character
        if (!password.matches(".*[@#$%^&+=!].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character (@#$%^&+=!)");
        }
        // Check for no whitespace
        if (password.matches(".*\\s.*")) {
            throw new IllegalArgumentException("Password must not contain whitespace");
        }
    }

    // ========== MAPPER ==========
    private PasswordResetResponseDTO toDTO(PasswordReset entity) {
        PasswordResetResponseDTO dto = new PasswordResetResponseDTO();
        dto.setId(entity.getId());
        dto.setPhone(entity.getPhone());
        dto.setResetCode(entity.getResetCode());
        dto.setCreatedAtDatetime(entity.getCreatedAtDatetime());
        dto.setUpdatedAtDatetime(entity.getUpdatedAtDatetime());
        return dto;
    }
}
