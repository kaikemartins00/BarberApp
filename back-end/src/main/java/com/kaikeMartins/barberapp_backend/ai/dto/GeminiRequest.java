package com.kaikeMartins.barberapp_backend.ai.dto;

import java.util.List;

public record GeminiRequest(List<GeminiContent> contents) {}
