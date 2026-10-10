package dev.joaomanoel.arvore.util;

import static org.assertj.core.api.Assertions.assertThat;

import dev.joaomanoel.arvore.dto.DiaConsistenciaDTO;
import dev.joaomanoel.arvore.dto.DiarioConsistenciaDTO.NivelDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiarioCalculoTest {

    private static final LocalDate DIA1 = LocalDate.of(2026, 10, 5);
    private static final LocalDate DIA2 = DIA1.plusDays(1);
    private static final LocalDate DIA3 = DIA1.plusDays(2);

    @Test
    void devolveSempreOsCincoNiveis() {
        List<NivelDTO> niveis = DiarioCalculo.porNivel(Map.of(), List.of());

        assertThat(niveis).extracting(NivelDTO::nivel).containsExactly(1, 2, 3, 4, 5);
        assertThat(niveis).allSatisfy(nivel -> {
            assertThat(nivel.dias()).isZero();
            assertThat(nivel.taxa()).isNull();
        });
    }

    @Test
    void somaPlanejadoEConcluidoDosDiasDeCadaNivel() {
        List<DiaConsistenciaDTO> dias = List.of(
                new DiaConsistenciaDTO(DIA1, 4, 1),
                new DiaConsistenciaDTO(DIA2, 4, 4),
                new DiaConsistenciaDTO(DIA3, 2, 2));

        List<NivelDTO> niveis = DiarioCalculo.porNivel(Map.of(DIA1, 2, DIA2, 5, DIA3, 5), dias);

        assertThat(niveis.get(1)).isEqualTo(new NivelDTO(2, 1, 4, 1, 25));
        assertThat(niveis.get(4)).isEqualTo(new NivelDTO(5, 2, 6, 6, 100));
        assertThat(niveis.get(0).dias()).isZero();
    }

    @Test
    void diaSemPlanoContaComoDiaMasNaoMexeNaTaxa() {
        List<DiaConsistenciaDTO> dias = List.of(new DiaConsistenciaDTO(DIA1, 0, 0), new DiaConsistenciaDTO(DIA2, 2, 1));

        NivelDTO tres = DiarioCalculo.porNivel(Map.of(DIA1, 3, DIA2, 3), dias).get(2);

        assertThat(tres.dias()).isEqualTo(2);
        assertThat(tres.taxa()).isEqualTo(50);
    }

    @Test
    void diaSemRegistroNoDiarioFicaDeFora() {
        List<DiaConsistenciaDTO> dias = List.of(new DiaConsistenciaDTO(DIA1, 3, 0), new DiaConsistenciaDTO(DIA2, 3, 3));

        List<NivelDTO> niveis = DiarioCalculo.porNivel(Map.of(DIA2, 4), dias);

        assertThat(niveis.stream().mapToInt(NivelDTO::dias).sum()).isEqualTo(1);
        assertThat(niveis.get(3).taxa()).isEqualTo(100);
    }
}
