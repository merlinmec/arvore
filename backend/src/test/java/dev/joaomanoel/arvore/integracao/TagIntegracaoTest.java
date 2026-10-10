package dev.joaomanoel.arvore.integracao;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Tag que fica sem nenhuma nota some da lista e do banco. */
class TagIntegracaoTest extends IntegracaoTest {

    private String areaId;
    private String notaId;

    @BeforeEach
    void criarNotaComTags() throws Exception {
        areaId = criar("/api/areas", json("nome", "Estudos"));
        notaId = criar("/api/areas/" + areaId + "/notas", "{\"titulo\":\"Leituras\",\"corpo\":\"\",\"tags\":[\"livros\",\"metas\"]}");
        criar("/api/areas/" + areaId + "/notas", "{\"titulo\":\"Outra\",\"corpo\":\"\",\"tags\":[\"livros\"]}");
    }

    @Test
    void tirarATagDaUnicaNotaQueAUsavaApagaATag() throws Exception {
        mvc.perform(autenticado(get("/api/tags"))).andExpect(jsonPath("$", contains("livros", "metas")));

        mvc.perform(autenticado(patch("/api/areas/" + areaId + "/notas/" + notaId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Leituras\",\"corpo\":\"\",\"tags\":[\"livros\"]}"))
                .andExpect(status().isOk());

        mvc.perform(autenticado(get("/api/tags"))).andExpect(jsonPath("$", contains("livros")));
        Assertions.assertThat(totalDeTags()).isEqualTo(1);
    }

    @Test
    void excluirNotaApagaAsTagsQueSoElaUsava() throws Exception {
        mvc.perform(autenticado(delete("/api/areas/" + areaId + "/notas/" + notaId))).andExpect(status().isNoContent());

        mvc.perform(autenticado(get("/api/tags"))).andExpect(jsonPath("$", contains("livros")));
        Assertions.assertThat(totalDeTags()).isEqualTo(1);
    }

    @Test
    void excluirAreaApagaAsTagsDasNotasDela() throws Exception {
        mvc.perform(autenticado(delete("/api/areas/" + areaId))).andExpect(status().isNoContent());

        mvc.perform(autenticado(get("/api/tags"))).andExpect(jsonPath("$", empty()));
        Assertions.assertThat(totalDeTags()).isZero();
    }

    private int totalDeTags() {
        entityManager.flush();
        return jdbc.queryForObject("select count(*) from tag", Integer.class);
    }
}
