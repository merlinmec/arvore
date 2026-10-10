package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.DefinirLembreteRequest;
import dev.joaomanoel.arvore.service.AtividadeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lembrete diário de uma atividade: o horário fica no backend, e a notificação sai do navegador. */
@RestController
@RequestMapping("/api/atividades/{atividadeId}/lembrete")
public class LembreteController {

    private final AtividadeService atividadeService;

    public LembreteController(AtividadeService atividadeService) {
        this.atividadeService = atividadeService;
    }

    @PutMapping
    public ResponseEntity<Void> definir(
            @PathVariable UUID atividadeId, @Valid @RequestBody DefinirLembreteRequest requisicao) {
        atividadeService.definirLembrete(atividadeId, requisicao.horario().withSecond(0).withNano(0));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> remover(@PathVariable UUID atividadeId) {
        atividadeService.definirLembrete(atividadeId, null);
        return ResponseEntity.noContent().build();
    }
}
