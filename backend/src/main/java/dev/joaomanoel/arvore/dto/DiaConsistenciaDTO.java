package dev.joaomanoel.arvore.dto;

import java.time.LocalDate;

public record DiaConsistenciaDTO(LocalDate data, int planejado, int concluido) {}
