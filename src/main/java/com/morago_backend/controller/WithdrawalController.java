package com.morago_backend.controller;

import com.morago_backend.dto.dtoRequest.WithdrawalRequestDTO;
import com.morago_backend.dto.dtoResponse.WithdrawalResponseDTO;
import com.morago_backend.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/withdrawals")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Withdrawal - ADMIN/INTERPRETER", description = "APIs for managing withdrawals")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService service;
    private static final Logger logger = LoggerFactory.getLogger(WithdrawalController.class);

    @Operation(summary = "Request withdrawal (Translator only)")
    @PreAuthorize("hasRole('INTERPRETER')")
    @PostMapping("/request")
    public ResponseEntity<WithdrawalResponseDTO> requestWithdrawal(@Valid @RequestBody WithdrawalRequestDTO dto) {
        try {
            WithdrawalResponseDTO result = service.requestWithdrawal(
                    dto.getSum(), dto.getAccountNumber(), dto.getAccountHolder(), dto.getBankName());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error requesting withdrawal", e);
            throw e;
        }
    }

    @Operation(summary = "Approve or reject withdrawal (Admin only)")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @PutMapping("/{id}/status")
    public ResponseEntity<WithdrawalResponseDTO> approveOrReject(@PathVariable Long id,
                                                                 @RequestParam String status) {
        try {
            WithdrawalResponseDTO result = service.approveOrReject(id, status);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error updating withdrawal status", e);
            throw e;
        }
    }

    @Operation(summary = "Get all withdrawals (Admin only)")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping
    public ResponseEntity<List<WithdrawalResponseDTO>> getAll() {
        try {
            List<WithdrawalResponseDTO> list = service.findAll();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            logger.error("Error fetching withdrawals", e);
            throw e;
        }
    }

    @Operation(summary = "Get withdrawal by ID (Admin only)")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @GetMapping("/{id}")
    public ResponseEntity<WithdrawalResponseDTO> getById(@PathVariable Long id) {
        try {
            WithdrawalResponseDTO dto = service.findById(id)
                    .orElseThrow(() -> new RuntimeException("Withdrawal not found"));
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            logger.error("Error fetching withdrawal id={}", id, e);
            throw e;
        }
    }

    @Operation(summary = "Delete withdrawal by ID (Admin only)")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting withdrawal id={}", id, e);
            throw e;
        }
    }
}
