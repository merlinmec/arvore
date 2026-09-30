package dev.joaomanoel.arvore.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Soma dos valores das conclusões das atividades de um objetivo. */
public record AcumuladoPorObjetivo(UUID objetivoId, BigDecimal total) {}
