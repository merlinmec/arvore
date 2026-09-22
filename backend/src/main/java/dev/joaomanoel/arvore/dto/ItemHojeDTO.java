package dev.joaomanoel.arvore.dto;

import java.util.UUID;

public record ItemHojeDTO(
        UUID atividadeId,
        String atividadeNome,
        String objetivoDescricao,
        UUID areaId,
        String areaNome,
        String areaCorTema,
        boolean concluidoHoje) {}
