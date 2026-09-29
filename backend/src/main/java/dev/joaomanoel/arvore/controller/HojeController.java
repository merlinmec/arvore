package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.DiaHojeDTO;
import dev.joaomanoel.arvore.service.HojeService;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hoje")
public class HojeController {

    private final HojeService hojeService;

    public HojeController(HojeService hojeService) {
        this.hojeService = hojeService;
    }

    @GetMapping
    public DiaHojeDTO listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return hojeService.listar(data != null ? data : Datas.hoje());
    }
}
