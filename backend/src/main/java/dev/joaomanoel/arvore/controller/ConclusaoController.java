package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.service.ConclusaoService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atividades/{atividadeId}/conclusao")
public class ConclusaoController {

    private final ConclusaoService conclusaoService;

    public ConclusaoController(ConclusaoService conclusaoService) {
        this.conclusaoService = conclusaoService;
    }

    @PostMapping
    public ResponseEntity<Void> marcar(@PathVariable UUID atividadeId) {
        conclusaoService.marcarHoje(atividadeId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> desmarcar(@PathVariable UUID atividadeId) {
        conclusaoService.desmarcarHoje(atividadeId);
        return ResponseEntity.noContent().build();
    }
}
