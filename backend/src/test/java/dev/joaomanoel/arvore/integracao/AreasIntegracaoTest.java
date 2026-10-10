package dev.joaomanoel.arvore.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AreasIntegracaoTest extends IntegracaoTest {

    private final LocalDate hoje = Datas.hoje();
    private String saudeId;
    private String corridaId;
    private String estudosId;
    private String objetivoLerId;

    @BeforeEach
    void montarArvore() throws Exception {
        saudeId = criar("/api/areas", json("nome", "Saúde", "corTema", "#5fc489"));
        corridaId = criar("/api/areas", json("nome", "Corrida", "areaPaiId", saudeId));
        estudosId = criar("/api/areas", json("nome", "Estudos"));
        atividadeHoje(saudeId, "Cuidar do corpo", "Alongar");
        atividadeHoje(corridaId, "Correr 100 km", "Correr");
        objetivoLerId = atividadeHoje(estudosId, "Ler 12 livros", "Ler");
    }

    @Test
    void arvoreAninhaSubareasNaOrdemDeCriacao() throws Exception {
        mvc.perform(autenticado(get("/api/areas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nome").value("Saúde"))
                .andExpect(jsonPath("$[0].corTema").value("#5fc489"))
                .andExpect(jsonPath("$[0].filhos", hasSize(1)))
                .andExpect(jsonPath("$[0].filhos[0].nome").value("Corrida"))
                .andExpect(jsonPath("$[1].nome").value("Estudos"))
                .andExpect(jsonPath("$[1].ordem").value(1));
    }

    @Test
    void progressoDaSemanaNaoSomaAsSubareas() throws Exception {
        mvc.perform(autenticado(get("/api/areas")))
                // cada área conta só os objetivos diretos: Saúde não leva a Corrida junto
                .andExpect(jsonPath("$[0].progresso.planejado").value(1))
                .andExpect(jsonPath("$[0].filhos[0].progresso.planejado").value(1))
                .andExpect(jsonPath("$[1].progresso.planejado").value(1))
                .andExpect(jsonPath("$[1].progresso.percentual").value(0));
    }

    @Test
    void concluirAtualizaOProgressoDaArea() throws Exception {
        String lerId = atividadeDe(estudosId);
        mvc.perform(autenticado(post("/api/atividades/" + lerId + "/conclusao"))).andExpect(status().isCreated());

        mvc.perform(autenticado(get("/api/areas")))
                .andExpect(jsonPath("$[1].progresso.concluido").value(1))
                .andExpect(jsonPath("$[1].progresso.percentual").value(100));
    }

    @Test
    void objetivoPausadoNaoContaNoProgresso() throws Exception {
        mvc.perform(autenticado(patch("/api/areas/" + estudosId + "/objetivos/" + objetivoLerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("descricao", "Ler 12 livros", "status", "PAUSADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSADO"));

        mvc.perform(autenticado(get("/api/areas")))
                .andExpect(jsonPath("$[1].progresso.planejado").value(0))
                .andExpect(jsonPath("$[1].progresso.percentual").doesNotExist());
        mvc.perform(autenticado(get("/api/hoje"))).andExpect(jsonPath("$.itens", hasSize(2)));
    }

    @Test
    void editarAreaTrocaNomeECor() throws Exception {
        mvc.perform(autenticado(patch("/api/areas/" + saudeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Corpo", "corTema", "#4fb3a9")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Corpo"));

        mvc.perform(autenticado(get("/api/areas")))
                .andExpect(jsonPath("$[0].nome").value("Corpo"))
                .andExpect(jsonPath("$[0].corTema").value("#4fb3a9"));
    }

    @Test
    void editarComNomeVazioOuAreaInexistenteFalha() throws Exception {
        mvc.perform(autenticado(patch("/api/areas/" + saudeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "  ")))
                .andExpect(status().isBadRequest());
        mvc.perform(autenticado(patch("/api/areas/" + UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Fantasma")))
                .andExpect(status().isNotFound());
    }

    @Test
    void subareaComPaiInexistenteDa404() throws Exception {
        mvc.perform(autenticado(post("/api/areas"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nome", "Órfã", "areaPaiId", UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluirAreaLevaSubareasObjetivosENotasJunto() throws Exception {
        criar("/api/areas/" + corridaId + "/notas", json("titulo", "Treinos", "corpo", "intervalado"));

        mvc.perform(autenticado(delete("/api/areas/" + saudeId))).andExpect(status().isNoContent());

        mvc.perform(autenticado(get("/api/areas")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Estudos"));
        mvc.perform(autenticado(get("/api/hoje")))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].atividadeNome").value("Ler"));
        mvc.perform(autenticado(get("/api/areas/" + corridaId + "/notas"))).andExpect(jsonPath("$", hasSize(0)));
    }

    /** Cria objetivo e atividade agendada pro dia de hoje; devolve o id do objetivo. */
    private String atividadeHoje(String areaId, String objetivo, String nome) throws Exception {
        String objetivoId = criar("/api/areas/" + areaId + "/objetivos", json("descricao", objetivo));
        String atividadeId = criar("/api/objetivos/" + objetivoId + "/atividades", json("nome", nome));
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/" + Datas.diaSemanaDe(hoje))))
                .andExpect(status().isCreated());
        return objetivoId;
    }

    private String atividadeDe(String areaId) throws Exception {
        String resposta = mvc.perform(autenticado(get("/api/areas/" + areaId + "/objetivos")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(resposta, "$[0].atividades[0].id");
    }
}
