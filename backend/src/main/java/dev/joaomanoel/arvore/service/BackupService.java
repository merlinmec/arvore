package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.BackupDTO;
import dev.joaomanoel.arvore.dto.ResumoBackupDTO;
import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.entity.StatusObjetivo;
import dev.joaomanoel.arvore.util.Csv;
import dev.joaomanoel.arvore.util.Datas;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exportação (CSV das conclusões), backup completo em JSON e restauração. Vai direto no JDBC: a
 * restauração precisa gravar ids e datas de criação como vieram, e as entidades JPA preenchem
 * {@code criado_em} sozinhas no {@code @PrePersist}.
 */
@Service
public class BackupService {

    private static final LocalDate INICIO_DOS_TEMPOS = LocalDate.of(1970, 1, 1);

    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public BackupService(JdbcTemplate jdbc, EntityManager entityManager) {
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    /** Uma linha por conclusão (ou recaída, no hábito negativo), da mais antiga pra mais recente. */
    @Transactional(readOnly = true)
    public String conclusoesCsv(LocalDate inicio, LocalDate fim) {
        LocalDate de = inicio != null ? inicio : INICIO_DOS_TEMPOS;
        LocalDate ate = fim != null ? fim : Datas.hoje();
        if (de.isAfter(ate)) {
            throw new IllegalArgumentException("Início depois do fim");
        }
        StringBuilder csv = new StringBuilder(Csv.BOM);
        csv.append(Csv.linha(List.of("data", "area", "objetivo", "atividade", "registro", "valor", "unidade")));
        jdbc.query(
                """
                with recursive caminho (id, nome) as (
                    select id, nome::text from area where area_pai_id is null
                    union all
                    select a.id, c.nome || ' / ' || a.nome from area a join caminho c on a.area_pai_id = c.id
                )
                select c.data, cam.nome as area, o.descricao, at.nome as atividade, at.negativa,
                       c.valor, o.meta_unidade
                from conclusao c
                join atividade at on at.id = c.atividade_id
                join objetivo o on o.id = at.objetivo_id
                join caminho cam on cam.id = o.area_id
                where c.data between ? and ?
                order by c.data, cam.nome, at.nome
                """,
                linha -> {
                    BigDecimal valor = linha.getBigDecimal("valor");
                    csv.append(
                            Csv.linha(
                                    Arrays.asList(
                                            linha.getObject("data", LocalDate.class).toString(),
                                            linha.getString("area"),
                                            linha.getString("descricao"),
                                            linha.getString("atividade"),
                                            linha.getBoolean("negativa") ? "recaída" : "feito",
                                            Csv.numero(valor),
                                            valor == null ? null : linha.getString("meta_unidade"))));
                },
                de,
                ate);
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public BackupDTO exportar() {
        Map<UUID, List<String>> tagsPorNota = new HashMap<>();
        jdbc.query(
                "select nt.nota_id, t.nome from nota_tag nt join tag t on t.id = nt.tag_id order by t.nome",
                linha -> {
                    tagsPorNota
                            .computeIfAbsent(uuid(linha, "nota_id"), chave -> new ArrayList<>())
                            .add(linha.getString("nome"));
                });

        return new BackupDTO(
                BackupDTO.VERSAO_ATUAL,
                Instant.now(),
                jdbc.query(
                        "select * from area order by criado_em, id",
                        (linha, n) -> new BackupDTO.Area(
                                uuid(linha, "id"),
                                uuid(linha, "area_pai_id"),
                                linha.getString("nome"),
                                linha.getString("descricao_geral"),
                                linha.getString("cor_tema"),
                                linha.getString("wallpaper_url"),
                                linha.getInt("ordem"),
                                instante(linha, "criado_em"),
                                instante(linha, "atualizado_em"))),
                jdbc.query(
                        "select * from nota order by criado_em, id",
                        (linha, n) -> new BackupDTO.Nota(
                                uuid(linha, "id"),
                                uuid(linha, "area_id"),
                                linha.getString("titulo"),
                                linha.getString("corpo"),
                                tagsPorNota.getOrDefault(uuid(linha, "id"), List.of()),
                                instante(linha, "criado_em"),
                                instante(linha, "atualizado_em"))),
                jdbc.query(
                        "select * from objetivo order by criado_em, id",
                        (linha, n) -> new BackupDTO.Objetivo(
                                uuid(linha, "id"),
                                uuid(linha, "area_id"),
                                linha.getString("descricao"),
                                linha.getObject("prazo", LocalDate.class),
                                linha.getString("status"),
                                linha.getBigDecimal("meta_valor"),
                                linha.getString("meta_unidade"),
                                instante(linha, "criado_em"),
                                instante(linha, "atualizado_em"))),
                jdbc.query(
                        "select * from atividade order by criado_em, id",
                        (linha, n) -> new BackupDTO.Atividade(
                                uuid(linha, "id"),
                                uuid(linha, "objetivo_id"),
                                linha.getString("nome"),
                                linha.getBoolean("negativa"),
                                instante(linha, "criado_em"),
                                instante(linha, "atualizado_em"))),
                jdbc.query(
                        "select * from agenda_semanal order by vigente_de, id",
                        (linha, n) -> new BackupDTO.Agenda(
                                uuid(linha, "id"),
                                uuid(linha, "atividade_id"),
                                linha.getString("dia_semana"),
                                linha.getObject("vigente_de", LocalDate.class),
                                linha.getObject("vigente_ate", LocalDate.class),
                                instante(linha, "criado_em"))),
                jdbc.query(
                        "select * from frequencia_semanal order by vigente_de, id",
                        (linha, n) -> new BackupDTO.Frequencia(
                                uuid(linha, "id"),
                                uuid(linha, "atividade_id"),
                                linha.getInt("vezes"),
                                linha.getObject("vigente_de", LocalDate.class),
                                linha.getObject("vigente_ate", LocalDate.class),
                                instante(linha, "criado_em"))),
                jdbc.query(
                        "select * from conclusao order by data, id",
                        (linha, n) -> new BackupDTO.Conclusao(
                                uuid(linha, "id"),
                                uuid(linha, "atividade_id"),
                                linha.getObject("data", LocalDate.class),
                                linha.getBigDecimal("valor"),
                                instante(linha, "criado_em"))),
                jdbc.query(
                        "select * from revisao_semanal order by semana",
                        (linha, n) -> new BackupDTO.Revisao(
                                uuid(linha, "id"),
                                linha.getObject("semana", LocalDate.class),
                                linha.getString("nota"),
                                instante(linha, "criado_em"),
                                instante(linha, "atualizado_em"))),
                jdbc.query(
                        "select * from diario order by data",
                        (linha, n) -> new BackupDTO.Diario(
                                uuid(linha, "id"),
                                linha.getObject("data", LocalDate.class),
                                linha.getObject("humor", Integer.class),
                                linha.getObject("energia", Integer.class),
                                linha.getString("nota"),
                                instante(linha, "criado_em"),
                                instante(linha, "atualizado_em"))),
                jdbc.query(
                        "select * from sessao_foco order by inicio, id",
                        (linha, n) -> new BackupDTO.SessaoFoco(
                                uuid(linha, "id"),
                                uuid(linha, "atividade_id"),
                                instante(linha, "inicio"),
                                instante(linha, "fim"))));
    }

    /**
     * Apaga tudo e grava o backup no lugar, numa transação só: qualquer inconsistência (pai que não
     * existe, valor fora da regra do banco) desfaz a operação inteira e os dados de antes ficam.
     */
    @Transactional
    public ResumoBackupDTO restaurar(BackupDTO backup) {
        if (backup.versao() == null || backup.versao() != BackupDTO.VERSAO_ATUAL) {
            throw new IllegalArgumentException(
                    "Formato de backup não suportado (versão " + backup.versao() + ", esperada " + BackupDTO.VERSAO_ATUAL + ")");
        }
        List<BackupDTO.Area> areas = paisAntes(lista(backup.areas()));
        List<BackupDTO.Nota> notas = lista(backup.notas());
        List<BackupDTO.Objetivo> objetivos = lista(backup.objetivos());
        List<BackupDTO.Atividade> atividades = lista(backup.atividades());
        List<BackupDTO.Conclusao> conclusoes = lista(backup.conclusoes());
        List<BackupDTO.Revisao> revisoes = lista(backup.revisoes());
        List<BackupDTO.Diario> diario = lista(backup.diario());
        List<BackupDTO.Agenda> agendas = lista(backup.agendas());
        List<BackupDTO.SessaoFoco> sessoesFoco = lista(backup.sessoesFoco());
        // o banco guarda texto livre nessas colunas; valor estranho quebraria a leitura pelo JPA depois
        objetivos.forEach(objetivo -> valorDoEnum(StatusObjetivo.class, objetivo.status(), "status"));
        agendas.forEach(agenda -> valorDoEnum(DiaSemana.class, agenda.diaSemana(), "dia da semana"));

        // o que o JPA tiver pendente vai antes, e o que ele tiver em cache some depois
        entityManager.flush();
        try {
            // area leva junto, em cascata, notas, objetivos, atividades, grades, conclusões e sessões de foco
            jdbc.update("delete from area");
            jdbc.update("delete from tag");
            jdbc.update("delete from revisao_semanal");
            jdbc.update("delete from diario");

            inserir(
                    "insert into area (id, area_pai_id, nome, descricao_geral, cor_tema, wallpaper_url, ordem, criado_em, atualizado_em) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    areas,
                    a -> new Object[] {
                        a.id(), a.areaPaiId(), a.nome(), a.descricaoGeral(), a.corTema(), a.wallpaperUrl(), a.ordem() == null ? 0 : a.ordem(),
                        ts(a.criadoEm()), ts(a.atualizadoEm())
                    });
            inserir(
                    "insert into nota (id, area_id, titulo, corpo, criado_em, atualizado_em) values (?, ?, ?, ?, ?, ?)",
                    notas,
                    n -> new Object[] {
                        n.id(), n.areaId(), n.titulo(), n.corpo() == null ? "" : n.corpo(), ts(n.criadoEm()), ts(n.atualizadoEm())
                    });
            restaurarTags(notas);
            inserir(
                    "insert into objetivo (id, area_id, descricao, prazo, status, meta_valor, meta_unidade, criado_em, atualizado_em) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    objetivos,
                    o -> new Object[] {
                        o.id(), o.areaId(), o.descricao(), o.prazo(), o.status() == null ? "ATIVO" : o.status(),
                        o.metaValor(), o.metaUnidade(), ts(o.criadoEm()), ts(o.atualizadoEm())
                    });
            inserir(
                    "insert into atividade (id, objetivo_id, nome, negativa, criado_em, atualizado_em) values (?, ?, ?, ?, ?, ?)",
                    atividades,
                    a -> new Object[] {
                        a.id(), a.objetivoId(), a.nome(), Boolean.TRUE.equals(a.negativa()), ts(a.criadoEm()), ts(a.atualizadoEm())
                    });
            inserir(
                    "insert into agenda_semanal (id, atividade_id, dia_semana, vigente_de, vigente_ate, criado_em) values (?, ?, ?, ?, ?, ?)",
                    agendas,
                    g -> new Object[] {g.id(), g.atividadeId(), g.diaSemana(), g.vigenteDe(), g.vigenteAte(), ts(g.criadoEm())});
            inserir(
                    "insert into frequencia_semanal (id, atividade_id, vezes, vigente_de, vigente_ate, criado_em) values (?, ?, ?, ?, ?, ?)",
                    lista(backup.frequencias()),
                    f -> new Object[] {f.id(), f.atividadeId(), f.vezes(), f.vigenteDe(), f.vigenteAte(), ts(f.criadoEm())});
            inserir(
                    "insert into conclusao (id, atividade_id, data, valor, criado_em) values (?, ?, ?, ?, ?)",
                    conclusoes,
                    c -> new Object[] {c.id(), c.atividadeId(), c.data(), c.valor(), ts(c.criadoEm())});
            inserir(
                    "insert into revisao_semanal (id, semana, nota, criado_em, atualizado_em) values (?, ?, ?, ?, ?)",
                    revisoes,
                    r -> new Object[] {r.id(), r.semana(), r.nota(), ts(r.criadoEm()), ts(r.atualizadoEm())});
            inserir(
                    "insert into diario (id, data, humor, energia, nota, criado_em, atualizado_em) values (?, ?, ?, ?, ?, ?, ?)",
                    diario,
                    d -> new Object[] {d.id(), d.data(), d.humor(), d.energia(), d.nota(), ts(d.criadoEm()), ts(d.atualizadoEm())});
            inserir(
                    "insert into sessao_foco (id, atividade_id, inicio, fim) values (?, ?, ?, ?)",
                    sessoesFoco,
                    s -> new Object[] {s.id(), s.atividadeId(), ts(s.inicio()), s.fim() == null ? null : Timestamp.from(s.fim())});
        } catch (DataIntegrityViolationException erro) {
            throw new IllegalArgumentException("Backup inconsistente: " + causaRaiz(erro));
        }
        entityManager.clear();

        return new ResumoBackupDTO(
                areas.size(),
                notas.size(),
                objetivos.size(),
                atividades.size(),
                conclusoes.size(),
                revisoes.size(),
                diario.size(),
                sessoesFoco.size());
    }

    /**
     * Ordena as áreas pra que cada pai venha antes dos filhos (a chave estrangeira é checada linha a
     * linha). Pai ausente do backup ou ciclo tornam o backup inválido.
     */
    static List<BackupDTO.Area> paisAntes(List<BackupDTO.Area> areas) {
        Map<UUID, BackupDTO.Area> porId = new LinkedHashMap<>();
        for (BackupDTO.Area area : areas) {
            if (area.id() == null || porId.put(area.id(), area) != null) {
                throw new IllegalArgumentException("Backup inconsistente: área sem id ou repetida");
            }
        }
        List<BackupDTO.Area> ordenadas = new ArrayList<>(areas.size());
        Set<UUID> colocadas = new HashSet<>();
        for (BackupDTO.Area area : areas) {
            colocar(area, porId, colocadas, new HashSet<>(), ordenadas);
        }
        return ordenadas;
    }

    private static void colocar(
            BackupDTO.Area area,
            Map<UUID, BackupDTO.Area> porId,
            Set<UUID> colocadas,
            Set<UUID> noCaminho,
            List<BackupDTO.Area> ordenadas) {
        if (colocadas.contains(area.id())) {
            return;
        }
        if (!noCaminho.add(area.id())) {
            throw new IllegalArgumentException("Backup inconsistente: as áreas formam um ciclo");
        }
        if (area.areaPaiId() != null) {
            BackupDTO.Area pai = porId.get(area.areaPaiId());
            if (pai == null) {
                throw new IllegalArgumentException("Backup inconsistente: área \"" + area.nome() + "\" com pai ausente");
            }
            colocar(pai, porId, colocadas, noCaminho, ordenadas);
        }
        colocadas.add(area.id());
        ordenadas.add(area);
    }

    private static <E extends Enum<E>> void valorDoEnum(Class<E> tipo, String valor, String campo) {
        if (valor == null) {
            return;
        }
        try {
            Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException erro) {
            throw new IllegalArgumentException("Backup inconsistente: " + campo + " inválido (" + valor + ")");
        }
    }

    private void restaurarTags(List<BackupDTO.Nota> notas) {
        Map<String, UUID> tags = new LinkedHashMap<>();
        List<Object[]> ligacoes = new ArrayList<>();
        for (BackupDTO.Nota nota : notas) {
            for (String nome : lista(nota.tags())) {
                String limpo = nome == null ? "" : nome.trim();
                if (limpo.isEmpty()) {
                    continue;
                }
                UUID tagId = tags.computeIfAbsent(limpo, chave -> UUID.randomUUID());
                ligacoes.add(new Object[] {nota.id(), tagId});
            }
        }
        inserir("insert into tag (id, nome) values (?, ?)", List.copyOf(tags.entrySet()), e -> new Object[] {e.getValue(), e.getKey()});
        jdbc.batchUpdate("insert into nota_tag (nota_id, tag_id) values (?, ?) on conflict do nothing", ligacoes);
    }

    private <T> void inserir(String sql, List<T> itens, Function<T, Object[]> colunas) {
        if (!itens.isEmpty()) {
            jdbc.batchUpdate(sql, itens.stream().map(colunas).toList());
        }
    }

    private static <T> List<T> lista(List<T> talvez) {
        return talvez == null ? List.of() : talvez;
    }

    private static Timestamp ts(Instant instante) {
        return Timestamp.from(instante != null ? instante : Instant.now());
    }

    private static UUID uuid(ResultSet linha, String coluna) throws SQLException {
        return linha.getObject(coluna, UUID.class);
    }

    private static Instant instante(ResultSet linha, String coluna) throws SQLException {
        Timestamp valor = linha.getTimestamp(coluna);
        return valor == null ? null : valor.toInstant();
    }

    private static String causaRaiz(Throwable erro) {
        Throwable causa = erro;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        String mensagem = causa.getMessage() == null ? "" : causa.getMessage();
        // o Postgres repete os dados da linha no "Detalhe"; a primeira linha basta
        int quebra = mensagem.indexOf('\n');
        return quebra > 0 ? mensagem.substring(0, quebra) : mensagem;
    }
}
