package com.morago_backend.dto.dtoResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Schema(description = "Response DTO for returning a TranslatorProfile with average rating")
public class TranslatorProfileWithRatingResponseDTO {

    @Schema(description = "ID of the translator", example = "1")
    private Long id;

    @Schema(description = "Phone of the translator", example = "08123456789")
    private String phone;

    @Schema(description = "Name of the translator", example = "John Doe")
    private String name;

    @Schema(description = "Date of birth", example = "1990-05-20")
    private LocalDate dateOfBirth;

    @Schema(description = "Is translator currently available", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Is translator currently online", example = "false")
    private Boolean isOnline;

    @Schema(description = "Is translator profile verified by admin", example = "false")
    private Boolean isVerified;

    @Schema(description = "Level of Korean proficiency", example = "Intermediate")
    private String levelOfKorean;

    @Schema(description = "List of themes the translator specializes in")
    private List<String> themes;

    @Schema(description = "Average rating of the translator", example = "4.50")
    private BigDecimal averageRating;

    @Schema(description = "Profile creation timestamp")
    private LocalDateTime createdAtDatetime;

    @Schema(description = "Profile last update timestamp")
    private LocalDateTime updatedAtDatetime;

    // Full constructor
    public TranslatorProfileWithRatingResponseDTO(Long id, String name, String phone, LocalDate dateOfBirth,
                                                  Boolean isAvailable, Boolean isOnline, Boolean isVerified,
                                                  String levelOfKorean, List<String> themes, BigDecimal averageRating,
                                                  LocalDateTime createdAtDatetime, LocalDateTime updatedAtDatetime) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.isAvailable = isAvailable;
        this.isOnline = isOnline;
        this.isVerified = isVerified;
        this.levelOfKorean = levelOfKorean;
        this.themes = themes;
        this.averageRating = averageRating;
        this.createdAtDatetime = createdAtDatetime;
        this.updatedAtDatetime = updatedAtDatetime;
    }
}
