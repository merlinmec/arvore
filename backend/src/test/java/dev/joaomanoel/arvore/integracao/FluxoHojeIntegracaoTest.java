package dev.joaomanoel.arvore.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Caminho principal do app: montar a árvore, agendar uma atividade e marcá-la na tela Hoje. */
class FluxoHojeIntegracaoTest extends IntegracaoTest {

    private final LocalDate hoje = Datas.hoje();
    private String areaId;
    private String atividadeId;

    @BeforeEach
    void montarArvore() throws Exception {
        areaId = criar("/api/areas", json("nome", "Saúde", "corTema", "#22c55e"));
        String objetivoId = criar("/api/areas/" + areaId + "/objetivos", json("descricao", "Correr 100 km"));
        atividadeId = criar("/api/objetivos/" + objetivoId + "/atividades", json("nome", "Correr"));
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/" + Datas.diaSemanaDe(hoje))))
                .andExpect(status().isCreated());
    }

    @Test
    void atividadeAgendadaApareceEmHojeSemConcluir() throws Exception {
        mvc.perform(autenticado(get("/api/hoje")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(hoje.toString()))
                .andExpect(jsonPath("$.editavel").value(true))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].atividadeId").value(atividadeId))
                .andExpect(jsonPath("$.itens[0].areaNome").value("Saúde"))
                .andExpect(jsonPath("$.itens[0].concluido").value(false))
                .andExpect(jsonPath("$.itens[0].streakAtual").value(0));
    }

    @Test
    void marcarConclusaoAtualizaHojeESequencia() throws Exception {
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/conclusao")))
                .andExpect(status().isCreated());

        mvc.perform(autenticado(get("/api/hoje")))
                .andExpect(jsonPath("$.itens[0].concluido").value(true))
                .andExpect(jsonPath("$.itens[0].streakAtual").value(1));
    }

    @Test
    void desmarcarConclusaoVoltaAoEstadoAnterior() throws Exception {
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/conclusao")))
                .andExpect(status().isCreated());
        mvc.perform(autenticado(delete("/api/atividades/" + atividadeId + "/conclusao")))
                .andExpect(status().isNoContent());

        mvc.perform(autenticado(get("/api/hoje")))
                .andExpect(jsonPath("$.itens[0].concluido").value(false));
    }

    @Test
    void conclusaoEmDataFuturaDa400() throws Exception {
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/conclusao")
                        .param("data", hoje.plusDays(1).toString())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void diaFuturoNaoEEditavel() throws Exception {
        mvc.perform(autenticado(get("/api/hoje").param("data", hoje.plusDays(1).toString())))
                .andExpect(jsonPath("$.editavel").value(false));
    }

    @Test
    void areaInexistenteDa404() throws Exception {
        mvc.perform(autenticado(post("/api/areas/" + UUID.randomUUID() + "/objetivos")
                        .contentType("application/json")
                        .content(json("descricao", "Órfão"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void objetivoInexistenteDa404() throws Exception {
        mvc.perform(autenticado(post("/api/objetivos/" + UUID.randomUUID() + "/atividades")
                        .contentType("application/json")
                        .content(json("nome", "Órfã"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void areaCriadaApareceNaArvore() throws Exception {
        mvc.perform(autenticado(get("/api/areas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + areaId + "')].nome").value("Saúde"));
    }
}
