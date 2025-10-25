package com.morago_backend.controller;

import com.morago_backend.dto.dtoRequest.RoomRequestDTO;
import com.morago_backend.dto.dtoResponse.RoomResponseDTO;
import com.morago_backend.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Room", description = "Endpoints for managing chat rooms")
public class RoomController {

    private final RoomService roomService;
    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ========== CREATE ROOM ==========
    @Operation(summary = "Create a new chat room", description = "Creates a new chat room with the provided details.")
    @PostMapping
    public ResponseEntity<RoomResponseDTO> createRoom(@RequestBody RoomRequestDTO request) {
        try {
            logger.info("Creating new room with data={}", request);
            RoomResponseDTO created = roomService.createRoom(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created); // 201 Created
        } catch (Exception ex) {
            logger.error("Error creating room: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== GET ROOM BY ID ==========
    @Operation(summary = "Get room by ID", description = "Retrieves a specific room using its unique roomId.")
    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponseDTO> getRoom(@PathVariable String roomId) {
        try {
            logger.info("Fetching room by roomId={}", roomId);
            RoomResponseDTO room = roomService.getRoom(roomId);
            return ResponseEntity.ok(room); // 200 OK
        } catch (Exception ex) {
            logger.error("Error fetching room {}: {}", roomId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
        }
    }

    // ========== GET ALL ROOMS ==========
    @Operation(summary = "Get all rooms", description = "Fetches a list of all available chat rooms.")
    @GetMapping
    public ResponseEntity<List<RoomResponseDTO>> getAllRooms() {
        try {
            logger.info("Fetching all rooms");
            List<RoomResponseDTO> rooms = roomService.getAllRooms();
            return ResponseEntity.ok(rooms); // 200 OK
        } catch (Exception ex) {
            logger.error("Error fetching all rooms: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

    // ========== UPDATE ROOM ==========
    @Operation(summary = "Update room", description = "Updates room information for a given roomId.")
    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponseDTO> updateRoom(@PathVariable String roomId,
                                                      @RequestBody RoomRequestDTO request) {
        try {
            logger.info("Updating room with roomId={} and data={}", roomId, request);
            RoomResponseDTO updated = roomService.updateRoom(roomId, request);
            return ResponseEntity.ok(updated); // 200 OK
        } catch (Exception ex) {
            logger.error("Error updating room {}: {}", roomId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== DELETE ROOM ==========
    @Operation(summary = "Delete room", description = "Deletes an existing room by its roomId.")
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable String roomId) {
        try {
            logger.info("Deleting room with roomId={}", roomId);
            roomService.deleteRoom(roomId);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (Exception ex) {
            logger.error("Error deleting room {}: {}", roomId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
