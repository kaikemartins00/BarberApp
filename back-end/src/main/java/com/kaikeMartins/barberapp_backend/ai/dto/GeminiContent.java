package com.kaikeMartins.barberapp_backend.ai.dto;

import java.util.List;

public record GeminiContent(String role, List<GeminiPart> parts) {}