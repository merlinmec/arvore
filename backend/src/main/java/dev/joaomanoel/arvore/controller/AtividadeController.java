package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.AtividadeDTO;
import dev.joaomanoel.arvore.dto.CriarAtividadeRequest;
import dev.joaomanoel.arvore.service.AtividadeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/objetivos/{objetivoId}/atividades")
public class AtividadeController {

    private final AtividadeService atividadeService;

    public AtividadeController(AtividadeService atividadeService) {
        this.atividadeService = atividadeService;
    }

    @PostMapping
    public ResponseEntity<AtividadeDTO> criar(
            @PathVariable UUID objetivoId, @Valid @RequestBody CriarAtividadeRequest requisicao) {
        AtividadeDTO criada = atividadeService.criar(objetivoId, requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @DeleteMapping("/{atividadeId}")
    public ResponseEntity<Void> excluir(@PathVariable UUID atividadeId) {
        atividadeService.excluir(atividadeId);
        return ResponseEntity.noContent().build();
    }
}
