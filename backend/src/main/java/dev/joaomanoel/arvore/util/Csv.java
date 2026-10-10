package dev.joaomanoel.arvore.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV no formato que o Excel em português abre direto: ponto e vírgula como separador, vírgula
 * decimal e BOM no início do arquivo (sem ele, o Excel lê UTF-8 como Latin-1).
 */
public final class Csv {

    public static final String BOM = "﻿";
    public static final char SEPARADOR = ';';

    private Csv() {}

    /** Uma linha terminada em CRLF, como pede a RFC 4180. Campo nulo vira vazio. */
    public static String linha(List<String> campos) {
        return campos.stream().map(Csv::campo).collect(Collectors.joining(String.valueOf(SEPARADOR))) + "\r\n";
    }

    /** Aspas só quando o campo tem separador, aspas ou quebra de linha; aspas internas dobram. */
    public static String campo(String valor) {
        if (valor == null) {
            return "";
        }
        boolean precisaAspas =
                valor.indexOf(SEPARADOR) >= 0
                        || valor.indexOf('"') >= 0
                        || valor.indexOf('\n') >= 0
                        || valor.indexOf('\r') >= 0;
        return precisaAspas ? '"' + valor.replace("\"", "\"\"") + '"' : valor;
    }

    /** "5.50" vira "5,5"; nulo vira vazio. */
    public static String numero(BigDecimal valor) {
        return valor == null ? null : valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
