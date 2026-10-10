package dev.joaomanoel.arvore.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Painel: três atividades diárias criadas há 20 dias. Na última semana, Alongar (Saúde) foi feito
 * nos 6 dias antes de hoje, Correr (Saúde / Corrida) nos 3 antes de hoje e Ler (Estudos) nunca.
 */
class PainelIntegracaoTest extends IntegracaoTest {

    private final LocalDate hoje = Datas.hoje();
    private String saudeId;
    private String corridaId;
    private String estudosId;
    private String objetivoLerId;

    @BeforeEach
    void montarHistorico() throws Exception {
        saudeId = criar("/api/areas", json("nome", "Saúde"));
        corridaId = criar("/api/areas", json("nome", "Corrida", "areaPaiId", saudeId));
        estudosId = criar("/api/areas", json("nome", "Estudos"));
        String alongar = atividadeDiaria(saudeId, "Cuidar do corpo", "Alongar");
        String correr = atividadeDiaria(corridaId, "Correr 100 km", "Correr");
        objetivoLerId = criar("/api/areas/" + estudosId + "/objetivos", json("descricao", "Ler 12 livros"));
        agendarTodoDia(criar("/api/objetivos/" + objetivoLerId + "/atividades", json("nome", "Ler")));
        recuarCriacao(20);

        for (int atras = 1; atras <= 6; atras++) concluir(alongar, hoje.minusDays(atras));
        for (int atras = 1; atras <= 3; atras++) concluir(correr, hoje.minusDays(atras));
    }

    @Test
    void resumoSomaPlanejadoEConcluidoDoPeriodo() throws Exception {
        mvc.perform(autenticado(get("/api/insights/resumo").param("dias", "7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value(hoje.minusDays(6).toString()))
                .andExpect(jsonPath("$.fim").value(hoje.toString()))
                .andExpect(jsonPath("$.planejado").value(21))
                .andExpect(jsonPath("$.concluido").value(9))
                .andExpect(jsonPath("$.taxa").value(43))
                // a semana anterior também tinha plano, e nada foi feito
                .andExpect(jsonPath("$.taxaPeriodoAnterior").value(0))
                .andExpect(jsonPath("$.diasComPlano").value(7))
                .andExpect(jsonPath("$.diasPerfeitos").value(0))
                .andExpect(jsonPath("$.serie", hasSize(7)))
                .andExpect(jsonPath("$.porDiaSemana", hasSize(7)));
    }

    @Test
    void rankingDeAtividadesVaiDaMaisParaAMenosConsistente() throws Exception {
        mvc.perform(autenticado(get("/api/insights/resumo").param("dias", "7")))
                .andExpect(jsonPath("$.atividades", hasSize(3)))
                .andExpect(jsonPath("$.atividades[0].nome").value("Alongar"))
                .andExpect(jsonPath("$.atividades[0].taxa").value(86))
                // hoje ainda pode ser feito: a sequência conta a partir de ontem
                .andExpect(jsonPath("$.atividades[0].streakAtual").value(6))
                .andExpect(jsonPath("$.atividades[0].maiorSequencia").value(6))
                .andExpect(jsonPath("$.atividades[1].nome").value("Correr"))
                .andExpect(jsonPath("$.atividades[1].taxa").value(43))
                .andExpect(jsonPath("$.atividades[2].nome").value("Ler"))
                .andExpect(jsonPath("$.atividades[2].taxa").value(0))
                .andExpect(jsonPath("$.atividades[2].streakAtual").value(0));
    }

    @Test
    void filtroDeAreaIncluiSubareasESeparaPorGalho() throws Exception {
        mvc.perform(autenticado(get("/api/insights/resumo").param("dias", "7").param("areaId", saudeId)))
                .andExpect(jsonPath("$.planejado").value(14))
                .andExpect(jsonPath("$.concluido").value(9))
                .andExpect(jsonPath("$.atividades", hasSize(2)))
                // com filtro, cada linha é um galho direto do filtro (ou o próprio filtro)
                .andExpect(jsonPath("$.porArea", hasSize(2)))
                .andExpect(jsonPath("$.porArea[?(@.areaId == '" + corridaId + "')].concluido").value(3));

        mvc.perform(autenticado(get("/api/insights/resumo").param("dias", "7").param("areaId", corridaId)))
                .andExpect(jsonPath("$.planejado").value(7))
                .andExpect(jsonPath("$.concluido").value(3));
        mvc.perform(autenticado(get("/api/insights/resumo").param("dias", "7").param("areaId", estudosId)))
                .andExpect(jsonPath("$.taxa").value(0));
    }

    @Test
    void semFiltroCadaLinhaDePorAreaEUmaAreaRaiz() throws Exception {
        mvc.perform(autenticado(get("/api/insights/resumo").param("dias", "7")))
                .andExpect(jsonPath("$.porArea", hasSize(2)))
                .andExpect(jsonPath("$.porArea[?(@.areaId == '" + saudeId + "')].planejado").value(14))
                .andExpect(jsonPath("$.porArea[?(@.areaId == '" + estudosId + "')].planejado").value(7));
    }

    @Test
    void objetivoPausadoSaiDoPainel() throws Exception {
        mvc.perform(autenticado(patch("/api/areas/" + estudosId + "/objetivos/" + objetivoLerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("descricao", "Ler 12 livros", "status", "PAUSADO")))
                .andExpect(status().isOk());

        mvc.perform(autenticado(get("/api/insights/resumo").param("dias", "7")))
                .andExpect(jsonPath("$.planejado").value(14))
                .andExpect(jsonPath("$.atividades", hasSize(2)));
    }

    @Test
    void historicoPadraoCobreAsUltimas53Semanas() throws Exception {
        int dias = 52 * 7 + hoje.getDayOfWeek().getValue();

        mvc.perform(autenticado(get("/api/insights/historico")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(dias)))
                .andExpect(jsonPath("$[0].data").value(Datas.inicioDaSemana(hoje).minusWeeks(52).toString()))
                .andExpect(jsonPath("$[" + (dias - 1) + "].data").value(hoje.toString()))
                // ontem: as três planejadas, Alongar e Correr feitas
                .andExpect(jsonPath("$[" + (dias - 2) + "].planejado").value(3))
                .andExpect(jsonPath("$[" + (dias - 2) + "].concluido").value(2))
                // antes da criação nada é planejado
                .andExpect(jsonPath("$[0].planejado").value(0));
    }

    @Test
    void historicoComPeriodoEArea() throws Exception {
        mvc.perform(autenticado(get("/api/insights/historico")
                        .param("inicio", hoje.minusDays(3).toString())
                        .param("fim", hoje.minusDays(1).toString())
                        .param("areaId", corridaId)))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].planejado").value(1))
                .andExpect(jsonPath("$[0].concluido").value(1));
    }

    @Test
    void parametrosInvalidosDao400E404() throws Exception {
        mvc.perform(autenticado(get("/api/insights/resumo").param("dias", "0"))).andExpect(status().isBadRequest());
        mvc.perform(autenticado(get("/api/insights/resumo").param("dias", "366"))).andExpect(status().isBadRequest());
        mvc.perform(autenticado(get("/api/insights/resumo").param("areaId", UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        mvc.perform(autenticado(get("/api/insights/historico")
                        .param("inicio", hoje.toString())
                        .param("fim", hoje.minusDays(1).toString())))
                .andExpect(status().isBadRequest());
        mvc.perform(autenticado(get("/api/insights/historico")
                        .param("inicio", hoje.minusDays(400).toString())
                        .param("fim", hoje.toString())))
                .andExpect(status().isBadRequest());
    }

    private String atividadeDiaria(String areaId, String objetivo, String nome) throws Exception {
        String objetivoId = criar("/api/areas/" + areaId + "/objetivos", json("descricao", objetivo));
        String atividadeId = criar("/api/objetivos/" + objetivoId + "/atividades", json("nome", nome));
        agendarTodoDia(atividadeId);
        return atividadeId;
    }

    private void agendarTodoDia(String atividadeId) throws Exception {
        for (DiaSemana dia : DiaSemana.values()) {
            mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/" + dia)))
                    .andExpect(status().isCreated());
        }
    }

    private void concluir(String atividadeId, LocalDate data) throws Exception {
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/conclusao").param("data", data.toString())))
                .andExpect(status().isCreated());
    }
}
