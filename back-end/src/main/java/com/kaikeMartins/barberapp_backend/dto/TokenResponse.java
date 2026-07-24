package com.kaikeMartins.barberapp_backend.dto;

public record TokenResponse(

        String token,
        long expirationTime
)
{}
