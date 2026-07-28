package com.playsphere.playsphere_backend.service;

import org.springframework.stereotype.Service;

import com.playsphere.playsphere_backend.dto.UserResponse;
import com.playsphere.playsphere_backend.entity.User;
import com.playsphere.playsphere_backend.repository.RegisterRequest;
import com.playsphere.playsphere_backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	
    @Override
    public UserResponse register(RegisterRequest request) {


        log.info("Register request received for email {}",
                request.getEmail());


        User user = User.builder()

                .name(request.getName())

                .email(request.getEmail())

                .password(
                    passwordEncoder.encode(request.getPassword())
                )

                .role(request.getRole())

                .build();



        User savedUser = userRepository.save(user);



        log.info("User registered successfully with id {}",
                savedUser.getId());



        return UserResponse.builder()

                .id(savedUser.getId())

                .name(savedUser.getName())

                .email(savedUser.getEmail())

                .role(savedUser.getRole())

                .build();

    }
	
	
	
	
	

}
