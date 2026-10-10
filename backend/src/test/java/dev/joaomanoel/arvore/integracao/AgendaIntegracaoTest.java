package dev.joaomanoel.arvore.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AgendaIntegracaoTest extends IntegracaoTest {

    private final LocalDate hoje = Datas.hoje();
    private String areaId;
    private String atividadeId;

    @BeforeEach
    void criarAtividadeSemGrade() throws Exception {
        areaId = criar("/api/areas", json("nome", "Saúde"));
        String objetivoId = criar("/api/areas/" + areaId + "/objetivos", json("descricao", "Correr 100 km"));
        atividadeId = criar("/api/objetivos/" + objetivoId + "/atividades", json("nome", "Correr"));
    }

    @Test
    void agendarDuasVezesNaoDuplica() throws Exception {
        agendar(DiaSemana.SEGUNDA).andExpect(status().isCreated());
        agendar(DiaSemana.SEGUNDA).andExpect(status().isCreated());
        agendar(DiaSemana.QUARTA).andExpect(status().isCreated());

        atividade()
                .andExpect(jsonPath("$[0].atividades[0].diasAgendados", contains("SEGUNDA", "QUARTA")))
                .andExpect(jsonPath("$[0].atividades[0].vezesPorSemana").doesNotExist());
        assertThat(linhasDeAgenda()).isEqualTo(2);
    }

    @Test
    void desmarcarNoMesmoDiaNaoDeixaRastro() throws Exception {
        agendar(DiaSemana.SEGUNDA).andExpect(status().isCreated());
        mvc.perform(autenticado(delete("/api/atividades/" + atividadeId + "/agenda/SEGUNDA")))
                .andExpect(status().isNoContent());

        atividade().andExpect(jsonPath("$[0].atividades[0].diasAgendados", empty()));
        assertThat(linhasDeAgenda()).isZero();
    }

    @Test
    void desmarcarDepoisEncerraAGradeSemReescreverOPassado() throws Exception {
        DiaSemana diaDeOntem = Datas.diaSemanaDe(hoje.minusDays(1));
        agendar(diaDeOntem).andExpect(status().isCreated());
        recuarCriacao(14);

        mvc.perform(autenticado(delete("/api/atividades/" + atividadeId + "/agenda/" + diaDeOntem)))
                .andExpect(status().isNoContent());

        atividade().andExpect(jsonPath("$[0].atividades[0].diasAgendados", empty()));
        // a linha fica, encerrada hoje: ontem continua planejado
        assertThat(linhasDeAgenda()).isEqualTo(1);
        mvc.perform(autenticado(get("/api/insights/historico")
                        .param("inicio", hoje.minusDays(1).toString())
                        .param("fim", hoje.minusDays(1).toString())))
                .andExpect(jsonPath("$[0].planejado").value(1));
        // e a semana que vem não tem mais o dia
        mvc.perform(autenticado(get("/api/hoje").param("data", hoje.plusDays(6).toString())))
                .andExpect(jsonPath("$.itens", empty()));
    }

    @Test
    void metaSemanalSubstituiOsDiasFixosEVoltaAtras() throws Exception {
        agendar(DiaSemana.SEGUNDA).andExpect(status().isCreated());

        frequencia(3).andExpect(status().isNoContent());
        atividade()
                .andExpect(jsonPath("$[0].atividades[0].vezesPorSemana").value(3))
                .andExpect(jsonPath("$[0].atividades[0].diasAgendados", empty()));
        // com a semana inteira coberta (criada antes da segunda), a meta vale cheia
        recuarCriacao(14);
        mvc.perform(autenticado(get("/api/hoje")))
                .andExpect(jsonPath("$.itens[0].metaSemanal").value(3))
                .andExpect(jsonPath("$.itens[0].feitasNaSemana").value(0));

        // marcar um dia fixo volta pro modo de dias fixos
        agendar(DiaSemana.TERCA).andExpect(status().isCreated());
        atividade()
                .andExpect(jsonPath("$[0].atividades[0].vezesPorSemana").doesNotExist())
                .andExpect(jsonPath("$[0].atividades[0].diasAgendados", contains("TERCA")));
    }

    @Test
    void removerMetaSemanalDeixaSemGrade() throws Exception {
        frequencia(2).andExpect(status().isNoContent());
        frequencia(4).andExpect(status().isNoContent());
        atividade().andExpect(jsonPath("$[0].atividades[0].vezesPorSemana").value(4));

        mvc.perform(autenticado(delete("/api/atividades/" + atividadeId + "/agenda/frequencia")))
                .andExpect(status().isNoContent());
        atividade()
                .andExpect(jsonPath("$[0].atividades[0].vezesPorSemana").doesNotExist())
                .andExpect(jsonPath("$[0].atividades[0].diasAgendados", empty()));
    }

    @Test
    void entradasInvalidasDao400E404() throws Exception {
        frequencia(0).andExpect(status().isBadRequest());
        frequencia(8).andExpect(status().isBadRequest());
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/FERIADO")))
                .andExpect(status().isBadRequest());
        mvc.perform(autenticado(post("/api/atividades/" + UUID.randomUUID() + "/agenda/SEGUNDA")))
                .andExpect(status().isNotFound());
        mvc.perform(autenticado(put("/api/atividades/" + UUID.randomUUID() + "/agenda/frequencia"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vezes\":3}"))
                .andExpect(status().isNotFound());
    }

    private ResultActions agendar(DiaSemana dia) throws Exception {
        return mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/" + dia)));
    }

    private ResultActions frequencia(int vezes) throws Exception {
        return mvc.perform(autenticado(put("/api/atividades/" + atividadeId + "/agenda/frequencia"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vezes\":" + vezes + "}"));
    }

    private ResultActions atividade() throws Exception {
        return mvc.perform(autenticado(get("/api/areas/" + areaId + "/objetivos"))).andExpect(status().isOk());
    }

    private int linhasDeAgenda() {
        entityManager.flush();
        return jdbc.queryForObject(
                "select count(*) from agenda_semanal where atividade_id = ?::uuid", Integer.class, atividadeId);
    }
}
