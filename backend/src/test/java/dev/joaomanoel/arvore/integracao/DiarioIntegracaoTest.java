package dev.joaomanoel.arvore.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class DiarioIntegracaoTest extends IntegracaoTest {

    private final LocalDate hoje = Datas.hoje();

    @Test
    void diaSemRegistroVemVazio() throws Exception {
        mvc.perform(autenticado(get("/api/diario/" + hoje)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(hoje.toString()))
                .andExpect(jsonPath("$.humor").doesNotExist())
                .andExpect(jsonPath("$.nota").doesNotExist());
    }

    @Test
    void salvaReescreveELista() throws Exception {
        salvar(hoje, "{\"humor\":4,\"energia\":2,\"nota\":\"  Dormi mal.  \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.humor").value(4))
                .andExpect(jsonPath("$.nota").value("Dormi mal."));
        // o registro é substituído por inteiro: a energia some
        salvar(hoje, "{\"humor\":5}").andExpect(status().isOk());
        salvar(hoje.minusDays(1), "{\"energia\":3}").andExpect(status().isOk());

        mvc.perform(autenticado(get("/api/diario/" + hoje)))
                .andExpect(jsonPath("$.humor").value(5))
                .andExpect(jsonPath("$.energia").doesNotExist())
                .andExpect(jsonPath("$.nota").doesNotExist());
        mvc.perform(autenticado(get("/api/diario")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].data").value(hoje.toString()));
    }

    @Test
    void salvarTudoVazioOuApagarRemoveORegistro() throws Exception {
        salvar(hoje, "{\"humor\":3}").andExpect(status().isOk());
        salvar(hoje, "{\"nota\":\"   \"}").andExpect(status().isOk());
        mvc.perform(autenticado(get("/api/diario"))).andExpect(jsonPath("$", hasSize(0)));

        salvar(hoje, "{\"humor\":3}").andExpect(status().isOk());
        mvc.perform(autenticado(delete("/api/diario/" + hoje))).andExpect(status().isNoContent());
        mvc.perform(autenticado(get("/api/diario"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void nivelForaDaEscalaOuDiaFuturoDa400() throws Exception {
        salvar(hoje, "{\"humor\":6}").andExpect(status().isBadRequest());
        salvar(hoje, "{\"energia\":0}").andExpect(status().isBadRequest());
        salvar(hoje.plusDays(1), "{\"humor\":3}").andExpect(status().isBadRequest());
    }

    @Test
    void cruzaHumorComAConsistenciaDoDia() throws Exception {
        String areaId = criar("/api/areas", json("nome", "Saúde"));
        String objetivoId = criar("/api/areas/" + areaId + "/objetivos", json("descricao", "Dormir bem"));
        String atividadeId = criar("/api/objetivos/" + objetivoId + "/atividades", json("nome", "Alongar"));
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/" + Datas.diaSemanaDe(hoje))))
                .andExpect(status().isCreated());
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/conclusao")))
                .andExpect(status().isCreated());
        salvar(hoje, "{\"humor\":4,\"energia\":5}").andExpect(status().isOk());

        mvc.perform(autenticado(get("/api/insights/diario").param("dias", "7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diasRegistrados").value(1))
                .andExpect(jsonPath("$.porHumor", hasSize(5)))
                .andExpect(jsonPath("$.porHumor[3].dias").value(1))
                .andExpect(jsonPath("$.porHumor[3].taxa").value(100))
                .andExpect(jsonPath("$.porEnergia[4].planejado").value(1))
                .andExpect(jsonPath("$.porHumor[0].taxa").doesNotExist());
    }

    private ResultActions salvar(LocalDate data, String corpo) throws Exception {
        return mvc.perform(autenticado(put("/api/diario/" + data))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }
}
