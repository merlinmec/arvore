package dev.joaomanoel.arvore.demo;

import static org.assertj.core.api.Assertions.assertThat;

import dev.joaomanoel.arvore.dto.BackupDTO;
import dev.joaomanoel.arvore.util.Datas;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DemoDadosTest {

    // uma quarta: a história começa na segunda nove semanas antes
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 14);
    private final BackupDTO dados = DemoDados.gerar(HOJE);

    @Test
    void nadaNoFuturoNemAntesDaCriacao() {
        LocalDate inicio = LocalDate.of(2026, 8, 10);
        assertThat(dados.conclusoes()).allSatisfy(c -> assertThat(c.data()).isBetween(inicio, HOJE));
        assertThat(dados.diario()).allSatisfy(d -> assertThat(d.data()).isBetween(inicio, HOJE.minusDays(1)));
        assertThat(dados.revisoes()).allSatisfy(r -> {
            assertThat(r.semana()).isBetween(inicio, Datas.inicioDaSemana(HOJE).minusWeeks(1));
            assertThat(r.semana().getDayOfWeek().getValue()).isEqualTo(1);
        });
        assertThat(dados.sessoesFoco()).allSatisfy(s -> assertThat(s.fim()).isAfter(s.inicio()).isBefore(Instant.now()));
        assertThat(dados.atividades()).allSatisfy(a -> assertThat(Datas.dataDe(a.criadoEm())).isEqualTo(inicio));
    }

    @Test
    void umaConclusaoPorAtividadePorDia() {
        Set<String> vistas = new HashSet<>();
        assertThat(dados.conclusoes()).allSatisfy(c -> assertThat(vistas.add(c.atividadeId() + "/" + c.data())).isTrue());
    }

    @Test
    void niveisDoDiarioDentroDaEscala() {
        assertThat(dados.diario()).isNotEmpty().allSatisfy(d -> {
            assertThat(d.humor()).isBetween(1, 5);
            assertThat(d.energia()).isBetween(1, 5);
        });
    }

    @Test
    void hojeTemQuaseTudoPorFazer() {
        assertThat(dados.conclusoes()).filteredOn(c -> c.data().equals(HOJE)).hasSize(1);
    }

    @Test
    void habitoNegativoTemRecaidasMasNaoTodoDia() {
        UUID negativo = dados.atividades().stream().filter(BackupDTO.Atividade::negativa).findFirst().orElseThrow().id();
        long recaidas = dados.conclusoes().stream().filter(c -> c.atividadeId().equals(negativo)).count();
        assertThat(recaidas).isBetween(3L, 30L);
    }

    @Test
    void gradeDaCorridaMudouNoMeioSemSobreposicao() {
        assertThat(dados.agendas())
                .filteredOn(g -> g.vigenteAte() != null)
                .isNotEmpty()
                .allSatisfy(g -> assertThat(g.vigenteAte()).isAfter(g.vigenteDe()));
    }
}
