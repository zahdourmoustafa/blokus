package com.blokus.blokus.service;

import com.blokus.blokus.dto.UserRegistrationDto;
import com.blokus.blokus.model.User;

public interface UserService {
    User register(UserRegistrationDto registrationDto);
    User findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
} 