package com.kaikeMartins.barberapp_backend.dto;

import java.time.LocalTime;
import java.util.List;

public record HorariosRequest(List<LocalTime> horarios) {}
