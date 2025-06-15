package org.spribe.booking.service;

import org.spribe.booking.dto.PageResponse;
import org.spribe.booking.dto.UserRequest;
import org.spribe.booking.dto.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse createUser(UserRequest request);
    
    UserResponse getUser(UUID id);
    
    UserResponse updateUser(UUID id, UserRequest request);
    
    void deleteUser(UUID id);

    PageResponse<UserResponse> getAllUsers(int page, int size);
}