package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.service.AgendaSemanalService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}
