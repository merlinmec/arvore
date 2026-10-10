package dev.joaomanoel.arvore.dto;

/** Quantos registros de cada tipo um backup restaurado trouxe. */
public record ResumoBackupDTO(
        int areas,
        int notas,
        int objetivos,
        int atividades,
        int conclusoes,
        int revisoes,
        int diario) {}
