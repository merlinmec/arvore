package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.DefinirFrequenciaRequest;
import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.service.AgendaSemanalService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atividades/{atividadeId}/agenda")
public class AgendaSemanalController {

    private final AgendaSemanalService agendaSemanalService;

    public AgendaSemanalController(AgendaSemanalService agendaSemanalService) {
        this.agendaSemanalService = agendaSemanalService;
    }

    @PostMapping("/{diaSemana}")
    public ResponseEntity<Void> agendar(
            @PathVariable UUID atividadeId, @PathVariable DiaSemana diaSemana) {
        agendaSemanalService.agendar(atividadeId, diaSemana);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{diaSemana}")
    public ResponseEntity<Void> desagendar(
            @PathVariable UUID atividadeId, @PathVariable DiaSemana diaSemana) {
        agendaSemanalService.desagendar(atividadeId, diaSemana);
        return ResponseEntity.noContent().build();
    }

    /** Troca pro modo "N vezes por semana": os dias fixos em vigor são encerrados. */
    @PutMapping("/frequencia")
    public ResponseEntity<Void> definirFrequencia(
            @PathVariable UUID atividadeId, @Valid @RequestBody DefinirFrequenciaRequest requisicao) {
        agendaSemanalService.definirFrequencia(atividadeId, requisicao.vezes());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/frequencia")
    public ResponseEntity<Void> removerFrequencia(@PathVariable UUID atividadeId) {
        agendaSemanalService.removerFrequencia(atividadeId);
        return ResponseEntity.noContent().build();
    }
}
