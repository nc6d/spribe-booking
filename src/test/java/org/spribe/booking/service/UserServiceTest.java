package org.spribe.booking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spribe.booking.config.TestContainersConfig;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.dto.UserRequest;
import org.spribe.booking.dto.UserResponse;
import org.spribe.booking.model.Event;
import org.spribe.booking.model.User;
import org.spribe.booking.model.enumeration.EventType;
import org.spribe.booking.model.enumeration.UserRole;
import org.spribe.booking.repository.EventRepository;
import org.spribe.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRequest validUserRequest;
    private User mockUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        validUserRequest = new UserRequest();
        validUserRequest.setEmail("test@example.com");
        validUserRequest.setFirstName("John");
        validUserRequest.setLastName("Doe");

        mockUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.USER)
                .build();
    }

    @Test
    void createUser_ValidRequest_ReturnsCreatedUser() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        UserResponse response = userService.createUser(validUserRequest);

        assertNotNull(response);
        assertEquals(testUserId, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals(UserRole.USER, response.getRole());

        verify(userRepository).save(any(User.class));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.USER_CREATED &&
            event.getEntityId().equals(testUserId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void createUser_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.createUser(validUserRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUser_ExistingUser_ReturnsUser() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(mockUser));

        UserResponse response = userService.getUser(testUserId);

        assertNotNull(response);
        assertEquals(testUserId, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
    }

    @Test
    void getUser_NonExistentUser_ThrowsException() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getUser(UUID.randomUUID()));
    }

    @Test
    void updateUser_ValidRequest_ReturnsUpdatedUser() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        UserResponse response = userService.updateUser(testUserId, validUserRequest);

        assertNotNull(response);
        assertEquals(testUserId, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());

        verify(userRepository).save(any(User.class));
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.USER_UPDATED &&
            event.getEntityId().equals(testUserId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void updateUser_NonExistentUser_ThrowsException() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, 
            () -> userService.updateUser(UUID.randomUUID(), validUserRequest));
    }

    @Test
    void updateUser_DuplicateEmail_ThrowsException() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, 
            () -> userService.updateUser(testUserId, validUserRequest));
    }

    @Test
    void deleteUser_ExistingUser_DeletesUser() {
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(mockUser));
        when(eventRepository.save(any(Event.class))).thenReturn(new Event());

        userService.deleteUser(testUserId);

        verify(userRepository).delete(mockUser);
        verify(eventRepository).save(argThat(event -> 
            event.getType() == EventType.USER_DELETED &&
            event.getEntityId().equals(testUserId) &&
            event.getUserId().equals(testUserId)
        ));
    }

    @Test
    void deleteUser_NonExistentUser_ThrowsException() {
        when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.deleteUser(UUID.randomUUID()));
    }

    @Test
    void getAllUsers_ReturnsPaginatedUsers() {
        Page<User> userPage = new PageImpl<>(Collections.singletonList(mockUser));
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(userPage);

        PageResponse<UserResponse> response = userService.getAllUsers(0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isLast());

        UserResponse userResponse = response.getContent().get(0);
        assertEquals(testUserId, userResponse.getId());
        assertEquals("test@example.com", userResponse.getEmail());
        assertEquals("John", userResponse.getFirstName());
        assertEquals("Doe", userResponse.getLastName());
    }
} 