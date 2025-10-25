package com.morago_backend.controller;

import com.morago_backend.dto.dtoRequest.TransactionFilterRequest;
import com.morago_backend.dto.dtoResponse.TransactionResponse;
import com.morago_backend.entity.TransactionType;
import com.morago_backend.entity.User;
import com.morago_backend.repository.UserRepository;
import com.morago_backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for managing and viewing financial transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    //====== GET MY TRANSACTIONS ======//
    @GetMapping("/my-transactions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my transactions", description = "Retrieve paginated list of transactions for the authenticated user")
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size
    ) {
        try {
            logger.info("Fetching transactions for current user, page={}, size={}", page, size);
            Long userId = getCurrentUserId();
            Page<TransactionResponse> transactions = transactionService.getUserTransactions(userId, page, size);
            return ResponseEntity.ok(transactions);
        } catch (Exception ex) {
            logger.error("Error fetching my transactions:", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //====== FILTER MY TRANSACTIONS ======//
    @PostMapping("/my-transactions/filter")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Filter my transactions", description = "Get filtered transactions based on type, status, or date range")
    public ResponseEntity<Page<TransactionResponse>> getFilteredTransactions(@RequestBody TransactionFilterRequest filter) {
        try {
            logger.info("Filtering transactions for current user with filter={}", filter);
            Long userId = getCurrentUserId();
            Page<TransactionResponse> transactions = transactionService.getFilteredTransactions(userId, filter);
            return ResponseEntity.ok(transactions);
        } catch (Exception ex) {
            logger.error("Error filtering my transactions:", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //====== GET TRANSACTION BY ID ======//
    @GetMapping("/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction by ID", description = "Retrieve details of a specific transaction")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long transactionId) {
        try {
            logger.info("Fetching transaction by id={}", transactionId);
            TransactionResponse transaction = transactionService.getTransactionById(transactionId);
            return ResponseEntity.ok(transaction);
        } catch (Exception ex) {
            logger.error("Error fetching transaction id={}:", transactionId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //====== GET USER TRANSACTIONS (ADMIN) ======//
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user transactions (Admin only)", description = "Retrieve paginated list of transactions for a specific user")
    public ResponseEntity<Page<TransactionResponse>> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            logger.info("Fetching transactions for user id={}, page={}, size={}", userId, page, size);
            Page<TransactionResponse> transactions = transactionService.getUserTransactions(userId, page, size);
            return ResponseEntity.ok(transactions);
        } catch (Exception ex) {
            logger.error("Error fetching transactions for user id={}:", userId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //====== FILTER USER TRANSACTIONS (ADMIN) ======//
    @PostMapping("/user/{userId}/filter")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Filter user transactions (Admin only)", description = "Get filtered transactions for a specific user")
    public ResponseEntity<Page<TransactionResponse>> filterUserTransactions(
            @PathVariable Long userId,
            @RequestBody TransactionFilterRequest filter
    ) {
        try {
            logger.info("Filtering transactions for user id={} with filter={}", userId, filter);
            Page<TransactionResponse> transactions = transactionService.getFilteredTransactions(userId, filter);
            return ResponseEntity.ok(transactions);
        } catch (Exception ex) {
            logger.error("Error filtering transactions for user id={}:", userId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //====== GET TOTAL BY TYPE ======//
    @GetMapping("/my-transactions/total/{type}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get total by transaction type", description = "Calculate total amount for a specific transaction type")
    public ResponseEntity<BigDecimal> getTotalByType(@PathVariable TransactionType type) {
        try {
            logger.info("Calculating total for current user by transaction type={}", type);
            Long userId = getCurrentUserId();
            BigDecimal total = transactionService.getTotalByType(userId, type);
            return ResponseEntity.ok(total);
        } catch (Exception ex) {
            logger.error("Error calculating total by type={}:", type, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //====== GET TRANSACTION COUNT ======//
    @GetMapping("/my-transactions/count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction count", description = "Get total number of transactions for the authenticated user")
    public ResponseEntity<Long> getTransactionCount() {
        try {
            logger.info("Counting transactions for current user");
            Long userId = getCurrentUserId();
            long count = transactionService.getTransactionCount(userId);
            return ResponseEntity.ok(count);
        } catch (Exception ex) {
            logger.error("Error counting transactions:", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //====== HELPER METHOD ======//
    private Long getCurrentUserId() {
        try {
            String phone = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));
            return user.getId();
        } catch (Exception ex) {
            logger.error("Error fetching current user ID:", ex);
            throw ex;
        }
    }
}
