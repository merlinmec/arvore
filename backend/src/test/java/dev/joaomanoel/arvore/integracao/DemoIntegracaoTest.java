package dev.joaomanoel.arvore.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import dev.joaomanoel.arvore.PostgresTestcontainer;
import dev.joaomanoel.arvore.demo.DemoService;
import dev.joaomanoel.arvore.security.JwtService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * A demonstração de ponta a ponta, com o perfil demo: cada visitante num schema próprio do Postgres.
 * Sem {@code @Transactional}: o Flyway e a restauração gravam de verdade, e cada teste apaga os
 * schemas que criou.
 */
@SpringBootTest(
        properties = {
            "arvore.jwt.segredo=segredo-dos-testes-de-integracao-0123456789",
            "arvore.demo.max-visitantes=6",
            "arvore.demo.por-ip-por-hora=2",
            "arvore.demo.aquecer=false",
            // pool pequeno: as mesmas conexões passam de um visitante pro outro o tempo todo
            "spring.datasource.hikari.maximum-pool-size=2"
        })
@AutoConfigureMockMvc
@Import(PostgresTestcontainer.class)
@ActiveProfiles("demo")
class DemoIntegracaoTest {

    private static int ultimoIp = 0;

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DemoService demoService;
    @Autowired JwtService jwtService;

    @AfterEach
    void apagarVisitantes() {
        jdbc.queryForList("select nspname from pg_namespace where nspname like 'visitante\\_%'", String.class)
                .forEach(esquema -> jdbc.execute("drop schema " + esquema + " cascade"));
    }

    @Test
    void statusMostraADemoLigada() throws Exception {
        mvc.perform(get("/api/demo/status")).andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void visitanteChegaComDezSemanasDeHistoria() throws Exception {
        String token = novoVisitante();

        mvc.perform(como(token, get("/api/areas")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].nome", hasItem("Saúde")))
                .andExpect(jsonPath("$[0].filhos", hasSize(2)));
        mvc.perform(como(token, get("/api/insights/resumo?dias=30")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planejado", greaterThan(30)))
                .andExpect(jsonPath("$.taxa", notNullValue()))
                .andExpect(jsonPath("$.taxaPeriodoAnterior", notNullValue()));
        mvc.perform(como(token, get("/api/hoje")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Meditar 10 min")));
        mvc.perform(como(token, get("/api/revisoes"))).andExpect(status().isOk()).andExpect(jsonPath("$", not(hasSize(0))));
        mvc.perform(como(token, get("/api/insights/tempo?dias=30")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Estudar 1 pomodoro")));
    }

    @Test
    void umVisitanteNaoEnxergaOOutro() throws Exception {
        String ana = novoVisitante();
        String bia = novoVisitante();

        mvc.perform(como(ana, post("/api/areas"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Só da Ana\"}"))
                .andExpect(status().isCreated());

        // várias vezes cada um, alternando: as conexões do pool (e os statements preparados nelas)
        // servem os dois, e o schema não pode vazar de um pro outro
        for (int i = 0; i < 8; i++) {
            mvc.perform(como(ana, get("/api/areas"))).andExpect(jsonPath("$", hasSize(5)));
            mvc.perform(como(bia, get("/api/areas")))
                    .andExpect(jsonPath("$", hasSize(4)))
                    .andExpect(jsonPath("$[*].nome", not(hasItem("Só da Ana"))));
        }
        // sem visitante, a conexão (que acabou de servir aos dois) volta pro public, que fica vazio
        assertThat(jdbc.queryForObject("select count(*) from area", Integer.class)).isZero();
    }

    @Test
    void restaurarBackupFicaDesligado() throws Exception {
        String token = novoVisitante();
        mvc.perform(como(token, put("/api/dados/backup")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro", containsString("desligado")));
    }

    @Test
    void tokenSemSchemaNaoEntraNaDemo() throws Exception {
        String tokenDeUsuario = jwtService.gerarToken("joao");
        mvc.perform(get("/api/areas").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDeUsuario))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginDeVerdadeNaoExisteNaDemo() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\":\"joao\",\"senha\":\"qualquer\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void limpezaApagaSoQuemPassouDe24Horas() throws Exception {
        String vencido = "visitante_" + Instant.now().minus(25, ChronoUnit.HOURS).getEpochSecond() + "_aaaaaaaaaaaa";
        jdbc.execute("create schema " + vencido);
        novoVisitante();

        demoService.limparVencidos();

        assertThat(visitantes()).hasSize(1).doesNotContain(vencido);
    }

    @Test
    void limitaVisitantesPorIp() throws Exception {
        String ip = proximoIp();
        criar(ip).andExpect(status().isOk());
        criar(ip).andExpect(status().isOk());
        criar(ip).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.erro", notNullValue()));
    }

    @Test
    void demoCheiaRecusaVisitanteNovo() throws Exception {
        long agora = Instant.now().getEpochSecond();
        for (int i = 0; i < 6; i++) {
            jdbc.execute("create schema visitante_" + agora + "_00000000000" + i);
        }
        criar(proximoIp()).andExpect(status().isServiceUnavailable());
    }

    @Test
    void rotaDoSpaCaiNoIndexEApiDesconhecidaNao() throws Exception {
        mvc.perform(get("/painel")).andExpect(status().isOk()).andExpect(content().string(containsString("spa-de-teste")));
        mvc.perform(get("/areas/123")).andExpect(status().isOk()).andExpect(content().string(containsString("spa-de-teste")));
        mvc.perform(get("/assets/nao-existe.js")).andExpect(status().isNotFound());
        mvc.perform(como(novoVisitante(), get("/api/nao-existe"))).andExpect(status().isNotFound());
    }

    private String novoVisitante() throws Exception {
        String resposta = criar(proximoIp()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    private org.springframework.test.web.servlet.ResultActions criar(String ip) throws Exception {
        return mvc.perform(post("/api/demo/sessao").with(requisicao -> {
            requisicao.setRemoteAddr(ip);
            return requisicao;
        }));
    }

    /** Cada visitante de um IP diferente, pra não esbarrar no limite por IP. */
    private static synchronized String proximoIp() {
        return "10.0.0." + (++ultimoIp);
    }

    private static MockHttpServletRequestBuilder como(String token, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private java.util.List<String> visitantes() {
        return jdbc.queryForList("select nspname from pg_namespace where nspname like 'visitante\\_%'", String.class);
    }
}
