package dev.joaomanoel.arvore.demo;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Na demonstração, cada visitante tem um schema próprio do Postgres, com as mesmas tabelas do app.
 * Este é o schema da requisição em andamento: o {@link DataSourceDoVisitante} aponta cada conexão
 * pra ele, então todo o resto do código (JPA, JDBC) continua sem saber que existe mais de um.
 *
 * <p>O nome carrega o instante da criação ({@code visitante_<segundos>_<aleatório>}): é dele que a
 * limpeza tira a idade, sem precisar de uma tabela de controle.
 */
public final class EsquemaDoVisitante {

    private static final Pattern NOME = Pattern.compile("visitante_(\\d{10})_[0-9a-f]{12}");
    private static final ThreadLocal<String> ATUAL = new ThreadLocal<>();

    private EsquemaDoVisitante() {}

    public static Optional<String> atual() {
        return Optional.ofNullable(ATUAL.get());
    }

    static void definir(String esquema) {
        if (!valido(esquema)) {
            throw new IllegalArgumentException("Schema de visitante inválido: " + esquema);
        }
        ATUAL.set(esquema);
    }

    static void limpar() {
        ATUAL.remove();
    }

    /** Roda {@code tarefa} como se fosse uma requisição do visitante dono de {@code esquema}. */
    static <T> T executarNo(String esquema, Supplier<T> tarefa) {
        String anterior = ATUAL.get();
        definir(esquema);
        try {
            return tarefa.get();
        } finally {
            if (anterior == null) limpar();
            else ATUAL.set(anterior);
        }
    }

    static String novo(Instant criadoEm, String aleatorio) {
        String esquema = "visitante_" + criadoEm.getEpochSecond() + "_" + aleatorio;
        if (!valido(esquema)) {
            throw new IllegalArgumentException("Schema de visitante inválido: " + esquema);
        }
        return esquema;
    }

    /**
     * Só nomes no formato exato passam: o nome vai parar em SQL (search_path, drop schema), e assim
     * nada que não foi gerado aqui chega lá.
     */
    static boolean valido(String esquema) {
        return esquema != null && NOME.matcher(esquema).matches();
    }

    static Instant criadoEm(String esquema) {
        Matcher partes = NOME.matcher(esquema);
        if (!partes.matches()) {
            throw new IllegalArgumentException("Schema de visitante inválido: " + esquema);
        }
        return Instant.ofEpochSecond(Long.parseLong(partes.group(1)));
    }
}
