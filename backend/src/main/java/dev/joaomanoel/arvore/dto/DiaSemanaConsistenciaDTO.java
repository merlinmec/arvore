package dev.joaomanoel.arvore.dto;

import dev.joaomanoel.arvore.entity.DiaSemana;

public record DiaSemanaConsistenciaDTO(DiaSemana dia, int planejado, int concluido, Integer taxa) {}
