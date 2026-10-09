package dev.joaomanoel.arvore.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AutenticacaoIntegracaoTest extends IntegracaoTest {

    @Test
    void senhaErradaDa401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("usuario", USUARIO, "senha", "errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioDesconhecidoDa401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("usuario", "outro", "senha", SENHA)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaProtegidaSemTokenDa401() throws Exception {
        mvc.perform(get("/api/areas")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDa401() throws Exception {
        mvc.perform(get("/api/areas").header(HttpHeaders.AUTHORIZATION, "Bearer nao-e-um-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDoLoginDaAcesso() throws Exception {
        mvc.perform(autenticado(get("/api/areas"))).andExpect(status().isOk());
    }
}
