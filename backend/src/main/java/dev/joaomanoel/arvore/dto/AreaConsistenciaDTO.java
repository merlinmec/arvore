package dev.joaomanoel.arvore.dto;

import java.util.UUID;

public record AreaConsistenciaDTO(
        UUID areaId, String nome, String corTema, int planejado, int concluido, Integer taxa) {}
