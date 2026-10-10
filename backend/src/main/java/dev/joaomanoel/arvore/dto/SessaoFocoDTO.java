package dev.joaomanoel.arvore.dto;

import java.time.Instant;
import java.util.UUID;

/** Uma sessão de foco; {@code fim} nulo é a que está em andamento, com os minutos contados até agora. */
public record SessaoFocoDTO(
        UUID id,
        UUID atividadeId,
        String atividadeNome,
        UUID areaId,
        String areaNome,
        String areaCorTema,
        Instant inicio,
        Instant fim,
        int minutos) {}
