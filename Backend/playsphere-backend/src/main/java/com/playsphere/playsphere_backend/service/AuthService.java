package com.playsphere.playsphere_backend.service;

import com.playsphere.playsphere_backend.dto.UserResponse;
import com.playsphere.playsphere_backend.repository.RegisterRequest;


public interface AuthService {
	UserResponse register(RegisterRequest request);
}
