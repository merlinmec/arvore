package dev.joaomanoel.arvore.util;

import static org.assertj.core.api.Assertions.assertThat;

import dev.joaomanoel.arvore.dto.ProgressoDTO;
import org.junit.jupiter.api.Test;

class ProgressoCalculoTest {

    @Test
    void semAtividadesAgendadasRetornaPercentualNulo() {
        ProgressoDTO progresso = ProgressoCalculo.calcular(0, 0);

        assertThat(progresso.planejado()).isZero();
        assertThat(progresso.concluido()).isZero();
        assertThat(progresso.percentual()).isNull();
    }

    @Test
    void nenhumaConclusaoNaSemanaRetornaZeroPorCento() {
        ProgressoDTO progresso = ProgressoCalculo.calcular(5, 0);

        assertThat(progresso.percentual()).isZero();
    }

    @Test
    void todasAsAtividadesConcluidasRetornaCemPorCento() {
        ProgressoDTO progresso = ProgressoCalculo.calcular(5, 5);

        assertThat(progresso.percentual()).isEqualTo(100);
    }

    @Test
    void metadeConcluidaRetornaCinquentaPorCento() {
        ProgressoDTO progresso = ProgressoCalculo.calcular(4, 2);

        assertThat(progresso.percentual()).isEqualTo(50);
    }

    @Test
    void arredondaPercentualQuebrado() {
        ProgressoDTO progresso = ProgressoCalculo.calcular(3, 1);

        assertThat(progresso.percentual()).isEqualTo(33);
    }

    @Test
    void concluidoAcimaDoPlanejadoFicaLimitadoEmCemPorCento() {
        ProgressoDTO progresso = ProgressoCalculo.calcular(2, 5);

        assertThat(progresso.percentual()).isEqualTo(100);
        assertThat(progresso.concluido()).isEqualTo(5);
    }
}
