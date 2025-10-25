package com.morago_backend.dto.dtoResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO for password change")
public class ChangePasswordResponseDTO {

    @Schema(description = "Success message", example = "Password changed successfully")
    private String message;

    @Schema(description = "User phone number", example = "+1234567890")
    private String phone;

    @Schema(description = "Timestamp of password change")
    private LocalDateTime changedAt;

    public ChangePasswordResponseDTO(String message) {
        this.message = message;
        this.changedAt = LocalDateTime.now();
    }
}

