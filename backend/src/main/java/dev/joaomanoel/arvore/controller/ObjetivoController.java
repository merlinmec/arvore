package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.CriarObjetivoRequest;
import dev.joaomanoel.arvore.dto.EditarObjetivoRequest;
import dev.joaomanoel.arvore.dto.ObjetivoComAtividadesDTO;
import dev.joaomanoel.arvore.dto.ObjetivoDTO;
import dev.joaomanoel.arvore.service.ObjetivoService;
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
@RequestMapping("/api/areas/{areaId}/objetivos")
public class ObjetivoController {

    private final ObjetivoService objetivoService;

    public ObjetivoController(ObjetivoService objetivoService) {
        this.objetivoService = objetivoService;
    }

    @GetMapping
    public List<ObjetivoComAtividadesDTO> listar(@PathVariable UUID areaId) {
        return objetivoService.listarPorArea(areaId);
    }

    @PostMapping
    public ResponseEntity<ObjetivoDTO> criar(
            @PathVariable UUID areaId, @Valid @RequestBody CriarObjetivoRequest requisicao) {
        ObjetivoDTO criado = objetivoService.criar(areaId, requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PatchMapping("/{objetivoId}")
    public ObjetivoDTO editar(
            @PathVariable UUID objetivoId, @Valid @RequestBody EditarObjetivoRequest requisicao) {
        return objetivoService.editar(objetivoId, requisicao);
    }

    @DeleteMapping("/{objetivoId}")
    public ResponseEntity<Void> excluir(@PathVariable UUID objetivoId) {
        objetivoService.excluir(objetivoId);
        return ResponseEntity.noContent().build();
    }
}
