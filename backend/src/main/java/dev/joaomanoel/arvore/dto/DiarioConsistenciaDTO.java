package dev.joaomanoel.arvore.dto;

import java.util.List;

/**
 * Consistência cruzada com o diário: pra cada nível de humor e de energia (1 a 5), quantos dias
 * tiveram esse nível e a taxa de conclusão somada desses dias.
 */
public record DiarioConsistenciaDTO(int diasRegistrados, List<NivelDTO> porHumor, List<NivelDTO> porEnergia) {

    public record NivelDTO(int nivel, int dias, int planejado, int concluido, Integer taxa) {}
}
