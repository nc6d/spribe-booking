package org.spribe.booking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.dto.UserRequest;
import org.spribe.booking.dto.UserResponse;
import org.spribe.booking.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "User", description = "User management APIs")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @Operation(summary = "Create a new user", description = "Creates a new user with the provided details")
    @PostMapping
    public UserResponse createUser(@RequestBody @Valid UserRequest request) {
        return userService.createUser(request);
    }

    @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID")
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        return userService.getUser(id);
    }

    @Operation(summary = "Update user", description = "Updates an existing user's details")
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return userService.updateUser(id, request);
    }

    @Operation(summary = "Delete user", description = "Deletes a user by their ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all users", description = "Retrieves a paginated list of all users")
    @GetMapping
    public PageResponse<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getAllUsers(page, size);
    }
} 