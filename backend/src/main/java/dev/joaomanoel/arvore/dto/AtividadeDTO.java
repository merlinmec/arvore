package dev.joaomanoel.arvore.dto;

import dev.joaomanoel.arvore.entity.DiaSemana;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * {@code vezesPorSemana} vem preenchido quando a atividade segue uma meta semanal em vez de dias
 * fixos. {@code negativa} marca um hábito negativo, que vale todo dia e não tem grade.
 */
public record AtividadeDTO(
        UUID id,
        UUID objetivoId,
        String nome,
        List<DiaSemana> diasAgendados,
        Integer vezesPorSemana,
        boolean negativa,
        // horário do lembrete diário, se houver
        LocalTime lembrete) {}
