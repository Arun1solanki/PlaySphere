package com.playsphere.playsphere_backend.dto;

import com.playsphere.playsphere_backend.entity.Role;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;

    private String name;

    private String email;

    private Role role;
}
