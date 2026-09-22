package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.AreaArvoreDTO;
import dev.joaomanoel.arvore.dto.AreaDTO;
import dev.joaomanoel.arvore.dto.CriarAreaRequest;
import dev.joaomanoel.arvore.dto.EditarAreaRequest;
import dev.joaomanoel.arvore.service.AreaService;
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
@RequestMapping("/api/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @GetMapping
    public List<AreaArvoreDTO> listar() {
        return areaService.listarArvore();
    }

    @PostMapping
    public ResponseEntity<AreaDTO> criar(@Valid @RequestBody CriarAreaRequest requisicao) {
        AreaDTO criada = areaService.criar(requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @PatchMapping("/{id}")
    public AreaDTO editar(@PathVariable UUID id, @Valid @RequestBody EditarAreaRequest requisicao) {
        return areaService.editar(id, requisicao);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        areaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
