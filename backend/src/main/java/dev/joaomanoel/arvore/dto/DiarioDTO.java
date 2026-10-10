package dev.joaomanoel.arvore.dto;

import java.time.LocalDate;

/** O diário de uma data. Dia sem registro vem com os três campos nulos. */
public record DiarioDTO(LocalDate data, Integer humor, Integer energia, String nota) {}
