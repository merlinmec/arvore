package dev.joaomanoel.arvore.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Uma revisão já feita, pro histórico de notas. */
public record RevisaoDTO(LocalDate semana, String nota, Instant atualizadoEm) {}
