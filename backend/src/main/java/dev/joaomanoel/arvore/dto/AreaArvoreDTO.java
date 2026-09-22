package dev.joaomanoel.arvore.dto;

import java.util.List;
import java.util.UUID;

public record AreaArvoreDTO(
        UUID id,
        String nome,
        String descricaoGeral,
        String corTema,
        String wallpaperUrl,
        int ordem,
        ProgressoDTO progresso,
        List<AreaArvoreDTO> filhos) {}
