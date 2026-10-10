package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.DiarioDTO;
import dev.joaomanoel.arvore.dto.SalvarDiarioRequest;
import dev.joaomanoel.arvore.service.DiarioService;
import dev.joaomanoel.arvore.util.Datas;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diario")
public class DiarioController {

    private final DiarioService diarioService;

    public DiarioController(DiarioService diarioService) {
        this.diarioService = diarioService;
    }

    /** Registros do período, do mais recente pro mais antigo. Sem datas, os últimos 30 dias. */
    @GetMapping
    public List<DiarioDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        LocalDate fimEfetivo = fim != null ? fim : Datas.hoje();
        LocalDate inicioEfetivo = inicio != null ? inicio : fimEfetivo.minusDays(29);
        return diarioService.listar(inicioEfetivo, fimEfetivo);
    }

    /** Dia sem registro devolve os campos nulos, não 404. */
    @GetMapping("/{data}")
    public DiarioDTO obter(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return diarioService.obter(data);
    }

    @PutMapping("/{data}")
    public DiarioDTO salvar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @Valid @RequestBody SalvarDiarioRequest requisicao) {
        return diarioService.salvar(data, requisicao);
    }

    @DeleteMapping("/{data}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        diarioService.apagar(data);
    }
}
