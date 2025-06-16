package org.spribe.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spribe.booking.config.TestContainersConfig;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.dto.UserRequest;
import org.spribe.booking.dto.UserResponse;
import org.spribe.booking.model.enumeration.UserRole;
import org.spribe.booking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserRequest validUserRequest;
    private UserResponse mockUserResponse;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        validUserRequest = new UserRequest();
        validUserRequest.setEmail("test@example.com");
        validUserRequest.setFirstName("John");
        validUserRequest.setLastName("Doe");

        mockUserResponse = new UserResponse();
        mockUserResponse.setId(testUserId);
        mockUserResponse.setEmail("test@example.com");
        mockUserResponse.setFirstName("John");
        mockUserResponse.setLastName("Doe");
        mockUserResponse.setRole(UserRole.USER);
    }

    @Test
    void createUser_ValidRequest_ReturnsCreatedUser() throws Exception {
        when(userService.createUser(any(UserRequest.class)))
                .thenReturn(mockUserResponse);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void getUser_ExistingUser_ReturnsUser() throws Exception {
        when(userService.getUser(testUserId)).thenReturn(mockUserResponse);

        mockMvc.perform(get("/api/v1/users/{id}", testUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void updateUser_ValidRequest_ReturnsUpdatedUser() throws Exception {
        when(userService.updateUser(any(UUID.class), any(UserRequest.class)))
                .thenReturn(mockUserResponse);

        mockMvc.perform(put("/api/v1/users/{id}", testUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void deleteUser_ExistingUser_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", testUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getAllUsers_ReturnsPaginatedUsers() throws Exception {
        PageResponse<UserResponse> pageResponse = new PageResponse<>(
                List.of(mockUserResponse),
                0,
                10,
                1,
                1,
                true
        );

        when(userService.getAllUsers(anyInt(), anyInt())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(testUserId.toString()))
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void createUser_InvalidRequest_ReturnsBadRequest() throws Exception {
        validUserRequest.setEmail("invalid-email"); // Invalid email format

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getUser_NonExistentUser_ReturnsNotFound() throws Exception {
        when(userService.getUser(any(UUID.class)))
                .thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }
} 