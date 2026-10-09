package dev.joaomanoel.arvore.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.joaomanoel.arvore.util.Datas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Hábito negativo ("não fumar"): vale todo dia sem grade, e o registro é a recaída. */
class HabitoNegativoIntegracaoTest extends IntegracaoTest {

    private String areaId;
    private String atividadeId;

    @BeforeEach
    void criarHabito() throws Exception {
        areaId = criar("/api/areas", json("nome", "Saúde"));
        String objetivoId = criar("/api/areas/" + areaId + "/objetivos", json("descricao", "Parar de fumar"));
        atividadeId = criar(
                "/api/objetivos/" + objetivoId + "/atividades",
                "{\"nome\":\"Não fumar\",\"negativa\":true}");
    }

    @Test
    void apareceEmHojeSemGradeEContaHojeComoLimpo() throws Exception {
        mvc.perform(autenticado(get("/api/hoje")))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].negativa").value(true))
                .andExpect(jsonPath("$.itens[0].concluido").value(false))
                .andExpect(jsonPath("$.itens[0].streakAtual").value(1));
    }

    @Test
    void recaidaZeraASequenciaEFalhaODia() throws Exception {
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/conclusao")))
                .andExpect(status().isCreated());

        mvc.perform(autenticado(get("/api/hoje")))
                .andExpect(jsonPath("$.itens[0].concluido").value(true))
                .andExpect(jsonPath("$.itens[0].streakAtual").value(0));

        String hoje = Datas.hoje().toString();
        mvc.perform(autenticado(get("/api/insights/historico").param("inicio", hoje).param("fim", hoje)))
                .andExpect(jsonPath("$[0].planejado").value(1))
                .andExpect(jsonPath("$[0].concluido").value(0));
    }

    @Test
    void naoAceitaGradeNemMetaSemanal() throws Exception {
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/SEGUNDA")))
                .andExpect(status().isBadRequest());
        mvc.perform(autenticado(put("/api/atividades/" + atividadeId + "/agenda/frequencia")
                        .contentType("application/json")
                        .content("{\"vezes\":3}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recaidaNaoRegistraValor() throws Exception {
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/conclusao").param("valor", "2")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaDeObjetivosMarcaAAtividadeComoNegativa() throws Exception {
        mvc.perform(autenticado(get("/api/areas/" + areaId + "/objetivos")))
                .andExpect(jsonPath("$[0].atividades[0].negativa").value(true))
                .andExpect(jsonPath("$[0].atividades[0].diasAgendados", hasSize(0)));
    }
}
