package dev.joaomanoel.arvore.dto;

import dev.joaomanoel.arvore.entity.StatusObjetivo;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ObjetivoComAtividadesDTO(
        UUID id,
        UUID areaId,
        String descricao,
        LocalDate prazo,
        StatusObjetivo status,
        ProgressoDTO progresso,
        MetaDTO meta,
        List<AtividadeDTO> atividades) {}
