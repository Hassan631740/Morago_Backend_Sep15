package com.morago_backend.controller;

import com.morago_backend.dto.dtoResponse.PasswordResetResponseDTO;
import com.morago_backend.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password-resets")
@Tag(name = "Password Reset Management - PUBLIC", description = "APIs for password reset (for users who forgot their password)")
public class PasswordResetController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);
    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    // ========== REQUEST RESET ==========
    @Operation(summary = "Request password reset - generates a 6-digit code valid for 15 minutes")
    @PostMapping("/request")
    public ResponseEntity<?> requestReset(@RequestParam String phone) {
        try {
            logger.info("Requesting password reset for phone={}", phone);
            PasswordResetResponseDTO dto = service.create(phone);
            logger.info("Password reset request successful for phone={}", phone);
            return ResponseEntity.status(201).body(dto);
        } catch (IllegalArgumentException ex) {
            logger.warn("Validation error requesting password reset: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error requesting password reset for phone={}: {}", phone, ex.getMessage(), ex);
            throw ex;
        }
    }

    // ========== VERIFY RESET CODE ==========
    @Operation(summary = "Verify password reset code (must be called before updating password)")
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestParam String phone,
                                        @RequestParam Integer code) {
        try {
            logger.info("Verifying password reset code for phone={}", phone);
            PasswordResetResponseDTO dto = service.verifyCode(phone, code);
            logger.info("Password reset code verified successfully for phone={}", phone);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            logger.warn("Validation error verifying reset code: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error verifying reset code for phone={}: {}", phone, ex.getMessage(), ex);
            throw ex;
        }
    }

    // ========== UPDATE PASSWORD ==========
    @Operation(summary = "Update password after code verification (requires verified reset session)")
    @PostMapping("/update-password")
    public ResponseEntity<?> updatePassword(
            @RequestParam String phone,
            @RequestParam String newPassword) {

        // Validate request parameters
        if (phone == null || phone.isBlank()) {
            logger.warn("Invalid request: phone is empty");
            throw new IllegalArgumentException("Phone number is required");
        }

        if (newPassword == null || newPassword.isBlank()) {
            logger.warn("Invalid request: newPassword is empty");
            throw new IllegalArgumentException("New password is required");
        }

        try {
            logger.info("Updating password for phone={}", phone);
            PasswordResetResponseDTO dto = service.updatePasswordMinimal(phone, newPassword);
            logger.info("Password updated successfully for phone={}", phone);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException ex) {
            logger.warn("Validation error updating password: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Error updating password for phone={}: {}", phone, ex.getMessage(), ex);
            throw ex;
        }
    }
}
