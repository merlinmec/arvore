package dev.joaomanoel.arvore.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.joaomanoel.arvore.util.Datas;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class BackupIntegracaoTest extends IntegracaoTest {

    private final LocalDate hoje = Datas.hoje();
    private String saudeId;
    private String corridaId;
    private String atividadeId;

    @BeforeEach
    void montarDados() throws Exception {
        saudeId = criar("/api/areas", json("nome", "Saúde"));
        corridaId = criar("/api/areas", json("nome", "Corrida", "areaPaiId", saudeId));
        String objetivoId = criar(
                "/api/areas/" + corridaId + "/objetivos",
                "{\"descricao\":\"Correr 100 km\",\"metaValor\":100,\"metaUnidade\":\"km\"}");
        atividadeId = criar("/api/objetivos/" + objetivoId + "/atividades", json("nome", "Correr; leve"));
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/agenda/" + Datas.diaSemanaDe(hoje))))
                .andExpect(status().isCreated());
        mvc.perform(autenticado(post("/api/atividades/" + atividadeId + "/conclusao").param("valor", "5.5")))
                .andExpect(status().isCreated());
        criar("/api/areas/" + saudeId + "/notas", "{\"titulo\":\"Treinos\",\"corpo\":\"intervalado\",\"tags\":[\"corrida\"]}");
        mvc.perform(autenticado(put("/api/diario/" + hoje))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"humor\":4,\"energia\":3,\"nota\":\"bom\"}"))
                .andExpect(status().isOk());
        mvc.perform(autenticado(put("/api/revisoes/" + hoje))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("nota", "Semana firme")))
                .andExpect(status().isOk());
    }

    @Test
    void csvTemBomCabecalhoEUmaLinhaPorConclusao() throws Exception {
        byte[] corpo = mvc.perform(autenticado(get("/api/dados/conclusoes.csv")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        String csv = new String(corpo, StandardCharsets.UTF_8);

        assertThat(csv).startsWith("﻿data;area;objetivo;atividade;registro;valor;unidade\r\n");
        // caminho completo da área, campo com ";" entre aspas e vírgula decimal
        assertThat(csv).contains(hoje + ";Saúde / Corrida;Correr 100 km;\"Correr; leve\";feito;5,5;km\r\n");
        assertThat(csv.split("\r\n")).hasSize(2);
    }

    @Test
    void csvRespeitaOPeriodo() throws Exception {
        String csv = mvc.perform(autenticado(get("/api/dados/conclusoes.csv")
                        .param("inicio", hoje.minusDays(10).toString())
                        .param("fim", hoje.minusDays(1).toString())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv.split("\r\n")).hasSize(1);
    }

    @Test
    void backupRestauradoVoltaExatamenteAoQueEra() throws Exception {
        String original = backup();

        // muda tudo depois do backup: área nova, nota apagada, diário reescrito
        criar("/api/areas", json("nome", "Lixo"));
        mvc.perform(autenticado(delete("/api/areas/" + corridaId))).andExpect(status().isNoContent());
        mvc.perform(autenticado(delete("/api/diario/" + hoje))).andExpect(status().isNoContent());

        restaurar(original)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.areas").value(2))
                .andExpect(jsonPath("$.atividades").value(1))
                .andExpect(jsonPath("$.conclusoes").value(1))
                .andExpect(jsonPath("$.notas").value(1))
                .andExpect(jsonPath("$.diario").value(1))
                .andExpect(jsonPath("$.revisoes").value(1));

        // ids, datas de criação e tags iguais: só o instante de geração muda
        assertThat(semGeradoEm(backup())).isEqualTo(semGeradoEm(original));
        mvc.perform(autenticado(get("/api/hoje")))
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].concluido").value(true))
                .andExpect(jsonPath("$.itens[0].valor").value(5.5));
        mvc.perform(autenticado(get("/api/tags"))).andExpect(jsonPath("$[0]").value("corrida"));
    }

    @Test
    void backupVazioApagaTudo() throws Exception {
        restaurar("{\"versao\":1}").andExpect(status().isOk()).andExpect(jsonPath("$.areas").value(0));

        mvc.perform(autenticado(get("/api/areas"))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(autenticado(get("/api/diario"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void versaoDesconhecidaOuAreaSemPaiDa400ENaoMudaNada() throws Exception {
        restaurar("{\"versao\":99}").andExpect(status().isBadRequest());
        restaurar("{\"versao\":1,\"areas\":[{\"id\":\"" + java.util.UUID.randomUUID()
                        + "\",\"areaPaiId\":\"" + java.util.UUID.randomUUID() + "\",\"nome\":\"Órfã\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.containsString("pai ausente")));
        restaurar("{\"versao\":1,\"agendas\":[{\"diaSemana\":\"FERIADO\"}]}").andExpect(status().isBadRequest());

        mvc.perform(autenticado(get("/api/areas"))).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void filhoAntesDoPaiNoArquivoTambemRestaura() throws Exception {
        String original = backup();
        // inverte a ordem das áreas no JSON: a subárea passa a vir primeiro
        int inicioAreas = original.indexOf("\"areas\":[") + 9;
        int fimAreas = original.indexOf("],\"notas\"");
        String areas = original.substring(inicioAreas, fimAreas);
        int corte = areas.indexOf("},{") + 1;
        String invertido = original.substring(0, inicioAreas)
                + areas.substring(corte + 1) + "," + areas.substring(0, corte)
                + original.substring(fimAreas);

        restaurar(invertido).andExpect(status().isOk()).andExpect(jsonPath("$.areas").value(2));
    }

    @Test
    void conclusaoDeAtividadeInexistenteDa400() throws Exception {
        // o banco recusa (chave estrangeira); fica por último porque a transação do teste aborta
        restaurar("{\"versao\":1,\"conclusoes\":[{\"id\":\"" + java.util.UUID.randomUUID()
                        + "\",\"atividadeId\":\"" + java.util.UUID.randomUUID() + "\",\"data\":\"" + hoje + "\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(org.hamcrest.Matchers.startsWith("Backup inconsistente")));
    }

    private String backup() throws Exception {
        return mvc.perform(autenticado(get("/api/dados/backup")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("arvore-backup-")))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private ResultActions restaurar(String corpo) throws Exception {
        return mvc.perform(autenticado(put("/api/dados/backup"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo));
    }

    private static String semGeradoEm(String json) {
        return json.replaceFirst("\"geradoEm\":\"[^\"]*\"", "");
    }
}
