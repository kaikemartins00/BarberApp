package com.kaikeMartins.barberapp_backend.event;

import java.util.List;

public record AgendamentosCanceladosEvent(List<AvisoCancelamento> avisos) {}