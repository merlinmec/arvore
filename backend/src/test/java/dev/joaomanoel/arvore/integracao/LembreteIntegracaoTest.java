package dev.joaomanoel.arvore.integracao;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.joaomanoel.arvore.util.Datas;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class LembreteIntegracaoTest extends IntegracaoTest {

    private String areaId;
    private String atividadeId;

    @BeforeEach
    void criarAtividadeDeHoje() throws Exception {
        areaId = criar("/api/areas", json("nome", "Saúde"));
        String objetivoId = criar("/api/areas/" + areaId + "/objetivos", json("descricao", "Dormir bem"));
        atividadeId = criar("/api/objetivos/" + objetivoId + "/atividades", json("nome", "Alongar"));
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/" + Datas.diaSemanaDe(Datas.hoje()))))
                .andExpect(status().isCreated());
    }

    @Test
    void lembreteApareceEmHojeENaListaDeObjetivos() throws Exception {
        mvc.perform(autenticado(get("/api/hoje"))).andExpect(jsonPath("$.itens[0].lembrete").doesNotExist());

        definir("07:30").andExpect(status().isNoContent());

        mvc.perform(autenticado(get("/api/hoje"))).andExpect(jsonPath("$.itens[0].lembrete").value(startsWith("07:30")));
        mvc.perform(autenticado(get("/api/areas/" + areaId + "/objetivos")))
                .andExpect(jsonPath("$[0].atividades[0].lembrete").value(startsWith("07:30")));
    }

    @Test
    void segundosSaoDescartadosERemoverLimpa() throws Exception {
        definir("21:15:42").andExpect(status().isNoContent());
        mvc.perform(autenticado(get("/api/hoje"))).andExpect(jsonPath("$.itens[0].lembrete").value(startsWith("21:15")));
        mvc.perform(autenticado(get("/api/hoje")))
                .andExpect(jsonPath("$.itens[0].lembrete").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(":42"))));

        mvc.perform(autenticado(delete("/api/atividades/" + atividadeId + "/lembrete"))).andExpect(status().isNoContent());
        mvc.perform(autenticado(get("/api/hoje"))).andExpect(jsonPath("$.itens[0].lembrete").doesNotExist());
    }

    @Test
    void horarioInvalidoOuAtividadeInexistente() throws Exception {
        definir("25:00").andExpect(status().isBadRequest());
        mvc.perform(autenticado(put("/api/atividades/" + atividadeId + "/lembrete"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(autenticado(put("/api/atividades/" + UUID.randomUUID() + "/lembrete"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("horario", "07:00")))
                .andExpect(status().isNotFound());
    }

    private ResultActions definir(String horario) throws Exception {
        return mvc.perform(autenticado(put("/api/atividades/" + atividadeId + "/lembrete"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("horario", horario)));
    }
}
