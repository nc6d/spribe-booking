package org.spribe.booking.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.dto.UserRequest;
import org.spribe.booking.dto.UserResponse;
import org.spribe.booking.model.Event;
import org.spribe.booking.model.enumeration.EventType;
import org.spribe.booking.model.User;
import org.spribe.booking.model.enumeration.UserRole;
import org.spribe.booking.repository.EventRepository;
import org.spribe.booking.repository.UserRepository;
import org.spribe.booking.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with this email already exists");
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(UserRole.USER)
                .build();
        
        user = userRepository.save(user);
        
        Event event = Event.builder()
                .type(EventType.USER_CREATED)
                .entityId(user.getId())
                .userId(user.getId())
                .description("User created: " + user.getId())
                .build();
        eventRepository.save(event);
        
        return objectMapper.convertValue(user, UserResponse.class);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        log.info("Getting user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return objectMapper.convertValue(user, UserResponse.class);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserRequest request) {
        log.info("Updating user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.getEmail().equals(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User with this email already exists");
        }
        
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        
        user = userRepository.save(user);
        
        Event event = Event.builder()
                .type(EventType.USER_UPDATED)
                .entityId(user.getId())
                .userId(user.getId())
                .description("User updated: " + user.getId())
                .build();
        eventRepository.save(event);
        
        return objectMapper.convertValue(user, UserResponse.class);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        log.info("Deleting user: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        userRepository.delete(user);
        
        Event event = Event.builder()
                .type(EventType.USER_DELETED)
                .entityId(user.getId())
                .userId(user.getId())
                .description("User deleted: " + user.getId())
                .build();
        eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        log.info("Getting all users, page: {}, size: {}", page, size);
        
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        
        List<UserResponse> userResponses = users.getContent().stream()
                .map(user -> objectMapper.convertValue(user, UserResponse.class))
                .toList();
        
        return new PageResponse<>(
                userResponses,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isLast()
        );
    }
}