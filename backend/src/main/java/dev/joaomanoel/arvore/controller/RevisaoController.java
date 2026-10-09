package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.RevisaoDTO;
import dev.joaomanoel.arvore.dto.RevisaoSemanaDTO;
import dev.joaomanoel.arvore.dto.SalvarRevisaoRequest;
import dev.joaomanoel.arvore.service.RevisaoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Revisão semanal. Qualquer data da semana serve como chave: ela é levada pra segunda-feira. */
@RestController
@RequestMapping("/api/revisoes")
public class RevisaoController {

    private final RevisaoService revisaoService;

    public RevisaoController(RevisaoService revisaoService) {
        this.revisaoService = revisaoService;
    }

    @GetMapping
    public List<RevisaoDTO> listar() {
        return revisaoService.listar();
    }

    @GetMapping("/{data}")
    public RevisaoSemanaDTO semana(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return revisaoService.semana(data);
    }

    @PutMapping("/{data}")
    public RevisaoDTO salvar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @Valid @RequestBody SalvarRevisaoRequest requisicao) {
        return revisaoService.salvar(data, requisicao.nota());
    }
}
