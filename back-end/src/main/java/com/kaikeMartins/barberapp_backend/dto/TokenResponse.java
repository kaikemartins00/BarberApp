package com.kaikeMartins.barberapp_backend.dto;

import java.util.List;

public record TokenResponse(

        String token,
        long expirationTime,
        Long userId,
        List<String> roles
)
{}
