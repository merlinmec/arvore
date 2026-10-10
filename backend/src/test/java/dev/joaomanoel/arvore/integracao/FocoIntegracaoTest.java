package dev.joaomanoel.arvore.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.joaomanoel.arvore.util.Datas;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class FocoIntegracaoTest extends IntegracaoTest {

    private final LocalDate hoje = Datas.hoje();
    private String saudeId;
    private String estudosId;
    private String correrId;
    private String lerId;

    @BeforeEach
    void montarAtividades() throws Exception {
        saudeId = criar("/api/areas", json("nome", "Saúde"));
        estudosId = criar("/api/areas", json("nome", "Estudos"));
        correrId = atividade(saudeId, "Correr");
        lerId = atividade(estudosId, "Ler");
    }

    @Test
    void iniciarMostraAtualEPararEncerra() throws Exception {
        String sessaoId = id(iniciar(correrId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.atividadeNome").value("Correr"))
                .andExpect(jsonPath("$.areaNome").value("Saúde"))
                .andExpect(jsonPath("$.fim").doesNotExist()));

        mvc.perform(autenticado(get("/api/foco/atual")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessaoId));
        mvc.perform(autenticado(post("/api/foco/parar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessaoId))
                .andExpect(jsonPath("$.fim").exists());
        mvc.perform(autenticado(get("/api/foco/atual"))).andExpect(status().isNoContent());
    }

    @Test
    void sessaoDeMenosDeUmMinutoEDescartadaAoParar() throws Exception {
        iniciar(correrId).andExpect(status().isCreated());
        mvc.perform(autenticado(post("/api/foco/parar"))).andExpect(status().isOk());

        mvc.perform(autenticado(get("/api/foco"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void iniciarOutraAtividadeEncerraAAnteriorEMesmaAtividadeNaoDuplica() throws Exception {
        String primeira = id(iniciar(correrId));
        iniciar(correrId).andExpect(jsonPath("$.id").value(primeira));
        // dez minutos de foco, pra a sessão não ser descartada como clique sem querer
        recuarInicioDaSessaoAberta(10);

        String segunda = id(iniciar(lerId));

        mvc.perform(autenticado(get("/api/foco/atual"))).andExpect(jsonPath("$.id").value(segunda));
        mvc.perform(autenticado(get("/api/foco")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id == '" + primeira + "')].fim").isNotEmpty());
    }

    @Test
    void tempoSomaPorDiaAreaEAtividadeDividindoNaMeiaNoite() throws Exception {
        LocalDate ontem = hoje.minusDays(1);
        LocalDate anteontem = hoje.minusDays(2);
        sessao(correrId, as(ontem, 10, 0), as(ontem, 10, 45));
        // das 23h30 de anteontem à 0h30 de ontem: meia hora em cada dia
        sessao(correrId, as(anteontem, 23, 30), as(ontem, 0, 30));
        sessao(lerId, as(ontem, 20, 0), as(ontem, 20, 20));
        // fora do período de 7 dias
        sessao(lerId, as(hoje.minusDays(20), 9, 0), as(hoje.minusDays(20), 10, 0));

        mvc.perform(autenticado(get("/api/insights/tempo").param("dias", "7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMinutos").value(125))
                .andExpect(jsonPath("$.sessoes").value(3))
                .andExpect(jsonPath("$.dias", hasSize(7)))
                .andExpect(jsonPath("$.dias[4].data").value(anteontem.toString()))
                .andExpect(jsonPath("$.dias[4].minutos").value(30))
                .andExpect(jsonPath("$.dias[5].minutos").value(95))
                .andExpect(jsonPath("$.porArea[0].nome").value("Saúde"))
                .andExpect(jsonPath("$.porArea[0].minutos").value(105))
                .andExpect(jsonPath("$.porArea[1].minutos").value(20))
                .andExpect(jsonPath("$.atividades[0].nome").value("Correr"))
                .andExpect(jsonPath("$.atividades[0].sessoes").value(2));

        mvc.perform(autenticado(get("/api/insights/tempo").param("dias", "7").param("areaId", estudosId)))
                .andExpect(jsonPath("$.totalMinutos").value(20))
                .andExpect(jsonPath("$.atividades", hasSize(1)));
        mvc.perform(autenticado(get("/api/insights/tempo").param("dias", "30")))
                .andExpect(jsonPath("$.totalMinutos").value(185));
    }

    @Test
    void sessaoEsquecidaAbertaEEncerradaEm12Horas() throws Exception {
        Instant inicio = as(hoje.minusDays(3), 8, 0);
        sessao(correrId, inicio, null);

        mvc.perform(autenticado(get("/api/foco/atual"))).andExpect(status().isNoContent());
        mvc.perform(autenticado(get("/api/insights/tempo").param("dias", "7")))
                .andExpect(jsonPath("$.totalMinutos").value(12 * 60));
        // e já dá pra começar outra
        iniciar(lerId).andExpect(status().isCreated());
    }

    @Test
    void apagarSessao() throws Exception {
        String sessaoId = id(iniciar(correrId));
        recuarInicioDaSessaoAberta(25);
        mvc.perform(autenticado(post("/api/foco/parar")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutos").value(25));

        mvc.perform(autenticado(delete("/api/foco/" + sessaoId))).andExpect(status().isNoContent());
        mvc.perform(autenticado(delete("/api/foco/" + sessaoId))).andExpect(status().isNotFound());
        mvc.perform(autenticado(get("/api/foco"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void entradasInvalidas() throws Exception {
        mvc.perform(autenticado(post("/api/foco/parar"))).andExpect(status().isNotFound());
        iniciar(UUID.randomUUID().toString()).andExpect(status().isNotFound());
        String objetivoId = criar("/api/areas/" + saudeId + "/objetivos", json("descricao", "Parar de fumar"));
        String negativa = criar("/api/objetivos/" + objetivoId + "/atividades", "{\"nome\":\"Não fumar\",\"negativa\":true}");
        iniciar(negativa).andExpect(status().isBadRequest());
        mvc.perform(autenticado(get("/api/insights/tempo").param("dias", "0"))).andExpect(status().isBadRequest());
        mvc.perform(autenticado(get("/api/insights/tempo").param("areaId", UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
    }

    private String atividade(String areaId, String nome) throws Exception {
        String objetivoId = criar("/api/areas/" + areaId + "/objetivos", json("descricao", "Objetivo de " + nome));
        return criar("/api/objetivos/" + objetivoId + "/atividades", json("nome", nome));
    }

    private ResultActions iniciar(String atividadeId) throws Exception {
        return mvc.perform(autenticado(post("/api/foco/iniciar"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("atividadeId", atividadeId)));
    }

    private static String id(ResultActions resultado) throws Exception {
        return JsonPath.read(resultado.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static Instant as(LocalDate dia, int hora, int minuto) {
        return dia.atTime(LocalTime.of(hora, minuto)).atZone(Datas.fuso()).toInstant();
    }

    private void recuarInicioDaSessaoAberta(int minutos) {
        entityManager.flush();
        jdbc.update("update sessao_foco set inicio = inicio - interval '" + minutos + " minutes' where fim is null");
        entityManager.clear();
    }

    private void sessao(String atividadeId, Instant inicio, Instant fim) {
        entityManager.flush();
        jdbc.update(
                "insert into sessao_foco (atividade_id, inicio, fim) values (?::uuid, ?, ?)",
                atividadeId,
                Timestamp.from(inicio),
                fim == null ? null : Timestamp.from(fim));
    }
}
