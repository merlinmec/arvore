package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.IniciarFocoRequest;
import dev.joaomanoel.arvore.dto.SessaoFocoDTO;
import dev.joaomanoel.arvore.service.FocoService;
import dev.joaomanoel.arvore.util.Datas;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Temporizador de foco: uma sessão por vez, iniciada e parada pela API. */
@RestController
@RequestMapping("/api/foco")
public class FocoController {

    private final FocoService focoService;

    public FocoController(FocoService focoService) {
        this.focoService = focoService;
    }

    /** A sessão em andamento, ou 204 se nenhuma estiver contando. */
    @GetMapping("/atual")
    public ResponseEntity<SessaoFocoDTO> atual() {
        return focoService.atual().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Começa a contar; se outra atividade estava contando, ela é encerrada agora. */
    @PostMapping("/iniciar")
    public ResponseEntity<SessaoFocoDTO> iniciar(@Valid @RequestBody IniciarFocoRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(focoService.iniciar(requisicao.atividadeId()));
    }

    @PostMapping("/parar")
    public SessaoFocoDTO parar() {
        return focoService.parar();
    }

    /** Sessões do período, da mais recente pra mais antiga. Sem datas, os últimos 7 dias. */
    @GetMapping
    public List<SessaoFocoDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        LocalDate fimEfetivo = fim != null ? fim : Datas.hoje();
        return focoService.listar(inicio != null ? inicio : fimEfetivo.minusDays(6), fimEfetivo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> apagar(@PathVariable UUID id) {
        focoService.apagar(id);
        return ResponseEntity.noContent().build();
    }
}
