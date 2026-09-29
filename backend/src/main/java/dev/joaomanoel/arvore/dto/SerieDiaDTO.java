package dev.joaomanoel.arvore.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SerieDiaDTO(
        LocalDate data, int planejado, int concluido, List<ContagemAreaDTO> concluidoPorArea) {

    public record ContagemAreaDTO(UUID areaId, int concluido) {}
}
