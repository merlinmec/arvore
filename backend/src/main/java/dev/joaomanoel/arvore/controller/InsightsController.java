package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.DiaConsistenciaDTO;
import dev.joaomanoel.arvore.dto.ResumoInsightsDTO;
import dev.joaomanoel.arvore.service.InsightsService;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/resumo")
    public ResumoInsightsDTO resumo(
            @RequestParam(defaultValue = "30") int dias, @RequestParam(required = false) UUID areaId) {
        return insightsService.resumo(dias, areaId);
    }

    /** Sem datas, devolve as últimas 53 semanas completas até hoje (grade do heatmap anual). */
    @GetMapping("/historico")
    public List<DiaConsistenciaDTO> historico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID areaId) {
        LocalDate fimEfetivo = fim != null ? fim : Datas.hoje();
        LocalDate inicioEfetivo =
                inicio != null ? inicio : Datas.inicioDaSemana(fimEfetivo).minusWeeks(52);
        return insightsService.historico(inicioEfetivo, fimEfetivo, areaId);
    }
}
