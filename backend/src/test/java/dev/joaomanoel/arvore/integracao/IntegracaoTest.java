package dev.joaomanoel.arvore.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.joaomanoel.arvore.PostgresTestcontainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base dos testes de integração: contexto completo, Postgres real (Testcontainers) e a cadeia de
 * segurança de verdade, autenticando pelo login com JWT. Cada teste roda numa transação desfeita no
 * fim, então um não enxerga os dados do outro.
 */
@SpringBootTest(
        properties = {
            "arvore.auth.usuario=" + IntegracaoTest.USUARIO,
            // bcrypt de "senha-teste-123"
            "arvore.auth.senha-hash=$2a$10$8aHGS200AN9NgzBeVi91Vebx0kbfiMmxPjDD2lNiPUJ3Cju3Bj9sS",
            "arvore.jwt.segredo=segredo-dos-testes-de-integracao-0123456789"
        })
@AutoConfigureMockMvc
@Import(PostgresTestcontainer.class)
@Transactional
abstract class IntegracaoTest {

    static final String USUARIO = "teste";
    static final String SENHA = "senha-teste-123";

    @Autowired MockMvc mvc;

    private String token;

    @BeforeEach
    void autenticar() throws Exception {
        String resposta = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("usuario", USUARIO, "senha", SENHA)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        token = JsonPath.read(resposta, "$.token");
    }

    /** Adiciona o token do usuário logado à requisição. */
    MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    /** Faz um POST autenticado com corpo JSON, espera 201 e devolve o {@code id} criado. */
    String criar(String url, String corpo) throws Exception {
        String resposta = mvc.perform(autenticado(post(url))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(resposta, "$.id");
    }

    /** JSON de um objeto plano a partir de pares chave/valor (só strings). */
    static String json(String... chavesEValores) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < chavesEValores.length; i += 2) {
            if (i > 0) json.append(',');
            json.append('"').append(chavesEValores[i]).append("\":\"").append(chavesEValores[i + 1]).append('"');
        }
        return json.append('}').toString();
    }
}
