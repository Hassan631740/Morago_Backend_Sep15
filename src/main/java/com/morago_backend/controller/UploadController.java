package com.morago_backend.controller;

import com.morago_backend.entity.File;
import com.morago_backend.entity.Theme;
import com.morago_backend.entity.User;
import com.morago_backend.exception.InvalidFileException;
import com.morago_backend.exception.ResourceNotFoundException;
import com.morago_backend.repository.FileRepository;
import com.morago_backend.repository.ThemeRepository;
import com.morago_backend.repository.UserRepository;
import com.morago_backend.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/uploads")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "File Upload/Download", description = "APIs for uploading and downloading files, avatars, and documents")
public class UploadController {

    private final StorageService storageService;
    private final ThemeRepository themeRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    
    // Allowed image types for avatars
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    // Allowed document types
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf", "image/jpeg", "image/jpg", "image/png",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    
    // Max file sizes (in bytes)
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10MB

    public UploadController(StorageService storageService, ThemeRepository themeRepository,
                            UserRepository userRepository, FileRepository fileRepository) {
        this.storageService = storageService;
        this.themeRepository = themeRepository;
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Get the currently authenticated user
     */
    private User getCurrentUser() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
    
    /**
     * Check if current user has admin role
     */
    private boolean isCurrentUserAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMINISTRATOR"));
    }
    
    /**
     * Validate if current user can perform action on target user
     * Users can only perform actions on their own account unless they are admin
     */
    private void validateUserAccess(Long targetUserId) {
        User currentUser = getCurrentUser();
        if (!isCurrentUserAdmin() && !currentUser.getId().equals(targetUserId)) {
            throw new AccessDeniedException("You can only upload files for your own account");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new InvalidFileException("File size exceeds maximum limit of 5MB");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new InvalidFileException("Invalid file type. Only JPEG, PNG, GIF, and WebP images are allowed");
        }
    }

    private void validateDocumentFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new InvalidFileException("File size exceeds maximum limit of 10MB");
        }
        if (!ALLOWED_DOCUMENT_TYPES.contains(file.getContentType())) {
            throw new InvalidFileException("Invalid file type. Only PDF, DOC, DOCX, JPEG, and PNG documents are allowed");
        }
    }

    // ========== UPLOAD THEME ICON ========== //
    @Operation(summary = "Upload theme icon (Admin only)")
    @PostMapping("/themes/{themeId}/icon")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<String> uploadThemeIcon(@PathVariable Long themeId,
                                                  @RequestParam("file") MultipartFile file) {
        try {
            logger.info("Uploading icon for theme id={} with filename={}", themeId, file.getOriginalFilename());
            
            // Validate file
            validateImageFile(file);
            
            Theme theme = themeRepository.findById(themeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Theme not found with id: " + themeId));

            String url = storageService.upload("theme-icons", file.getOriginalFilename(), file);
            File record = new File();
            record.setOriginalTitle(file.getOriginalFilename());
            record.setPath(url);
            record.setType(file.getContentType());
            record.setTheme(theme);
            File saved = fileRepository.save(record);

            theme.setIconId(saved.getId());
            themeRepository.save(theme);

            return ResponseEntity.created(URI.create(url)).body(url);
        } catch (Exception ex) {
            logger.error("Error uploading theme icon for themeId={}: {}", themeId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== UPLOAD USER AVATAR ========== //
    @Operation(summary = "Upload user avatar", description = "Upload avatar for your own account (or any user if admin)")
    @PostMapping("/users/{userId}/avatar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','INTERPRETER','CLIENT')")
    public ResponseEntity<String> uploadUserAvatar(@PathVariable Long userId,
                                                   @RequestParam("file") MultipartFile file) {
        try {
            logger.info("Uploading avatar for user id={} with filename={}", userId, file.getOriginalFilename());
            validateUserAccess(userId);
            validateImageFile(file);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            String url = storageService.upload("avatars", file.getOriginalFilename(), file);
            File saved = new File();
            saved.setOriginalTitle(file.getOriginalFilename());
            saved.setPath(url);
            saved.setType(file.getContentType());
            saved.setUser(getCurrentUser());
            saved = fileRepository.save(saved);

            user.setImageId(saved.getId());
            userRepository.save(user);

            return ResponseEntity.created(URI.create(url)).body(url);
        } catch (AccessDeniedException ex) {
            logger.error("Access denied for userId={}: {}", userId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Error uploading avatar for userId={}: {}", userId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== UPLOAD CERTIFICATE ========== //
    @Operation(summary = "Upload interpreter certificate", description = "Upload certificate for your own account (or any interpreter if admin)")
    @PostMapping("/users/{userId}/certificate")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','INTERPRETER')")
    public ResponseEntity<String> uploadCertificate(@PathVariable Long userId,
                                                    @RequestParam("file") MultipartFile file) {
        try {
            logger.info("Uploading certificate for user id={} with filename={}", userId, file.getOriginalFilename());
            validateUserAccess(userId);
            validateDocumentFile(file);

            userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            String url = storageService.upload("certificates", file.getOriginalFilename(), file);
            File saved = new File();
            saved.setOriginalTitle(file.getOriginalFilename());
            saved.setPath(url);
            saved.setType(file.getContentType());
            saved.setUser(getCurrentUser());
            fileRepository.save(saved);

            return ResponseEntity.created(URI.create(url)).body(url);
        } catch (AccessDeniedException ex) {
            logger.error("Access denied for userId={}: {}", userId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
        } catch (Exception ex) {
            logger.error("Error uploading certificate for userId={}: {}", userId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== DELETE UPLOADED FILE ========== //
    @Operation(summary = "Delete uploaded file", description = "Delete a file (Admin or file owner)")
    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','INTERPRETER','CLIENT')")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));

        Long ownerId = file.getUser() != null ? file.getUser().getId() : null;
        if (ownerId != null) {
            validateUserAccess(ownerId);
        } else if (!isCurrentUserAdmin()) {
            throw new AccessDeniedException("You can only delete your own files");
        }

        storageService.delete(file.getPath());
        fileRepository.delete(file);

        return ResponseEntity.noContent().build();
    }

}
