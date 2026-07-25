package com.playsphere.playsphere_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.playsphere.playsphere_backend.dto.UserResponse;
import com.playsphere.playsphere_backend.repository.RegisterRequest;
import com.playsphere.playsphere_backend.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;
	
	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(
			 @Valid @RequestBody RegisterRequest request){
		
		UserResponse response=authService.register(request);
		
		return new ResponseEntity<>(
                response,
                HttpStatus.CREATED
        );
		
		
	}
	

}
