package dev.joaomanoel.arvore.demo;

import dev.joaomanoel.arvore.security.JwtService;
import dev.joaomanoel.arvore.service.BackupService;
import dev.joaomanoel.arvore.util.Datas;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Cria e apaga os visitantes da demonstração. Cada um ganha um schema próprio, migrado pelo mesmo
 * Flyway do app e preenchido com {@link DemoDados} pela restauração de backup, e o schema inteiro é
 * apagado depois de {@link #DURACAO}. Como o servidor gratuito pode passar horas parado, a limpeza
 * roda de hora em hora e também antes de cada visitante novo.
 */
@Service
@Profile("demo")
public class DemoService {

    static final Duration DURACAO = Duration.ofHours(24);
    private static final Logger log = LoggerFactory.getLogger(DemoService.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final BackupService backupService;
    private final JwtService jwtService;
    private final int maxVisitantes;
    private final int porIpPorHora;
    private final boolean aquecer;
    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Deque<Instant>> criacoesPorIp = new ConcurrentHashMap<>();

    public DemoService(
            DataSource dataSource,
            JdbcTemplate jdbc,
            BackupService backupService,
            JwtService jwtService,
            @Value("${arvore.demo.max-visitantes}") int maxVisitantes,
            @Value("${arvore.demo.por-ip-por-hora}") int porIpPorHora,
            @Value("${arvore.demo.aquecer}") boolean aquecer) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
        this.backupService = backupService;
        this.jwtService = jwtService;
        this.maxVisitantes = maxVisitantes;
        this.porIpPorHora = porIpPorHora;
        this.aquecer = aquecer;
    }

    /** Cria o visitante e devolve o token dele. */
    public String criarVisitante(String ip) {
        limparVencidos();
        reservarVaga(ip);
        Instant agora = Instant.now();
        String esquema = EsquemaDoVisitante.novo(agora, HexFormat.of().formatHex(aleatorio.generateSeed(6)));
        preparar(esquema);
        return jwtService.gerarTokenDeVisitante(esquema, agora.plus(DURACAO));
    }

    private synchronized void reservarVaga(String ip) {
        Instant umaHoraAtras = Instant.now().minus(Duration.ofHours(1));
        criacoesPorIp.values().forEach(criacoes -> criacoes.removeIf(quando -> quando.isBefore(umaHoraAtras)));
        criacoesPorIp.values().removeIf(Deque::isEmpty);

        Deque<Instant> doIp = criacoesPorIp.computeIfAbsent(ip, chave -> new ArrayDeque<>());
        if (doIp.size() >= porIpPorHora) {
            throw new DemoIndisponivelException(
                    HttpStatus.TOO_MANY_REQUESTS, "Muitas demonstrações criadas daqui na última hora. Tente mais tarde.");
        }
        if (visitantes().size() >= maxVisitantes) {
            throw new DemoIndisponivelException(
                    HttpStatus.SERVICE_UNAVAILABLE, "A demonstração está cheia agora. Tente de novo em alguns minutos.");
        }
        doIp.add(Instant.now());
    }

    /** Migra o schema novo e grava os dados fictícios nele. Se algo falhar, não fica schema pela metade. */
    private void preparar(String esquema) {
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(esquema)
                    .createSchemas(true)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();
            EsquemaDoVisitante.executarNo(esquema, () -> backupService.restaurar(DemoDados.gerar(Datas.hoje())));
        } catch (RuntimeException erro) {
            apagar(esquema);
            throw erro;
        }
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    public void limparVencidos() {
        Instant limite = Instant.now().minus(DURACAO);
        for (String esquema : visitantes()) {
            if (EsquemaDoVisitante.criadoEm(esquema).isBefore(limite)) {
                apagar(esquema);
            }
        }
    }

    List<String> visitantes() {
        return jdbc.queryForList("select nspname from pg_namespace where nspname like 'visitante\\_%'", String.class).stream()
                .filter(EsquemaDoVisitante::valido)
                .toList();
    }

    private void apagar(String esquema) {
        // o nome passou pela validação: só letras, dígitos e "_" no formato gerado aqui
        if (EsquemaDoVisitante.valido(esquema)) {
            jdbc.execute("drop schema if exists " + esquema + " cascade");
        }
    }

    /**
     * Na subida, cria e apaga um visitante em segundo plano: o primeiro visitante de verdade não paga
     * o Flyway, o Hibernate e o JIT ainda frios.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void aoSubir() {
        if (!aquecer) return;
        Thread aquecimento = new Thread(() -> {
            try {
                limparVencidos();
                Instant inicio = Instant.now();
                String esquema = EsquemaDoVisitante.novo(inicio, HexFormat.of().formatHex(aleatorio.generateSeed(6)));
                preparar(esquema);
                apagar(esquema);
                log.info("Demonstração aquecida em {} ms", Duration.between(inicio, Instant.now()).toMillis());
            } catch (RuntimeException erro) {
                log.warn("Aquecimento da demonstração falhou", erro);
            }
        }, "aquecimento-demo");
        aquecimento.setDaemon(true);
        aquecimento.start();
    }
}
