package dev.joaomanoel.arvore.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class RevisaoIntegracaoTest extends IntegracaoTest {

    private final LocalDate hoje = Datas.hoje();
    private final LocalDate segunda = Datas.inicioDaSemana(hoje);
    private String atividadeId;

    @BeforeEach
    void montarArvore() throws Exception {
        String areaId = criar("/api/areas", json("nome", "Estudos"));
        String objetivoId = criar("/api/areas/" + areaId + "/objetivos", json("descricao", "Ler 12 livros"));
        atividadeId = criar("/api/objetivos/" + objetivoId + "/atividades", json("nome", "Ler"));
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/" + Datas.diaSemanaDe(hoje))))
                .andExpect(status().isCreated());
    }

    @Test
    void retratoDaSemanaVaiDaSegundaAteHoje() throws Exception {
        int diasAteHoje = (int) (hoje.toEpochDay() - segunda.toEpochDay()) + 1;

        mvc.perform(autenticado(get("/api/revisoes/" + hoje)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semana").value(segunda.toString()))
                .andExpect(jsonPath("$.fim").value(segunda.plusDays(6).toString()))
                .andExpect(jsonPath("$.apuradoAte").value(hoje.toString()))
                .andExpect(jsonPath("$.dias", hasSize(diasAteHoje)))
                .andExpect(jsonPath("$.planejado").value(1))
                .andExpect(jsonPath("$.concluido").value(0))
                .andExpect(jsonPath("$.nota").doesNotExist())
                .andExpect(jsonPath("$.atividades[0].atividade.id").value(atividadeId))
                .andExpect(jsonPath("$.atividades[0].atividade.diasAgendados[0]").value(Datas.diaSemanaDe(hoje).name()))
                // hoje ainda pode ser feito, então não conta como escapou
                .andExpect(jsonPath("$.atividades[0].diasPerdidos", hasSize(0)));
    }

    @Test
    void concluirAtualizaORetrato() throws Exception {
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/conclusao")))
                .andExpect(status().isCreated());

        mvc.perform(autenticado(get("/api/revisoes/" + segunda)))
                .andExpect(jsonPath("$.concluido").value(1))
                .andExpect(jsonPath("$.taxa").value(100))
                .andExpect(jsonPath("$.atividades[0].concluido").value(1));
    }

    @Test
    void notaESalvaPelaSemanaEReescrita() throws Exception {
        salvarNota(hoje, "Semana corrida, mas li todo dia.").andExpect(status().isOk())
                .andExpect(jsonPath("$.semana").value(segunda.toString()));
        salvarNota(segunda, "  Reescrita.  ").andExpect(status().isOk());

        mvc.perform(autenticado(get("/api/revisoes/" + hoje)))
                .andExpect(jsonPath("$.nota").value("Reescrita."));
        mvc.perform(autenticado(get("/api/revisoes")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].semana").value(segunda.toString()))
                .andExpect(jsonPath("$[0].nota").value("Reescrita."));
    }

    @Test
    void notaVaziaDa400() throws Exception {
        salvarNota(hoje, "   ").andExpect(status().isBadRequest());
    }

    @Test
    void semanaQueNaoComecouDa400() throws Exception {
        LocalDate proximaSegunda = segunda.plusWeeks(1);

        mvc.perform(autenticado(get("/api/revisoes/" + proximaSegunda))).andExpect(status().isBadRequest());
        salvarNota(proximaSegunda, "Cedo demais").andExpect(status().isBadRequest());
    }

    private ResultActions salvarNota(LocalDate data, String nota)
            throws Exception {
        return mvc.perform(autenticado(put("/api/revisoes/" + data))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("nota", nota)));
    }
}
