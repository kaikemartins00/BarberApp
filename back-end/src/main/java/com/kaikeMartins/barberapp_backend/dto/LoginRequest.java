package com.kaikeMartins.barberapp_backend.dto;

public record LoginRequest (
    String email,
    String senha
){}
