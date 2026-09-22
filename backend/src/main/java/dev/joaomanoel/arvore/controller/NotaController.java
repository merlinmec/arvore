package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.CriarNotaRequest;
import dev.joaomanoel.arvore.dto.NotaDTO;
import dev.joaomanoel.arvore.service.NotaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/areas/{areaId}/notas")
public class NotaController {

    private final NotaService notaService;

    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    @GetMapping
    public List<NotaDTO> listar(@PathVariable UUID areaId) {
        return notaService.listarPorArea(areaId);
    }

    @PostMapping
    public ResponseEntity<NotaDTO> criar(
            @PathVariable UUID areaId, @Valid @RequestBody CriarNotaRequest requisicao) {
        NotaDTO criada = notaService.criar(areaId, requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PatchMapping("/{notaId}")
    public NotaDTO editar(@PathVariable UUID notaId, @Valid @RequestBody CriarNotaRequest requisicao) {
        return notaService.editar(notaId, requisicao);
    }

    @DeleteMapping("/{notaId}")
    public ResponseEntity<Void> excluir(@PathVariable UUID notaId) {
        notaService.excluir(notaId);
        return ResponseEntity.noContent().build();
    }
}
