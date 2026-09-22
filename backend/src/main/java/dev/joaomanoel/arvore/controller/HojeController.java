package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.ItemHojeDTO;
import dev.joaomanoel.arvore.service.HojeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hoje")
public class HojeController {

    private final HojeService hojeService;

    public HojeController(HojeService hojeService) {
        this.hojeService = hojeService;
    }

    @GetMapping
    public List<ItemHojeDTO> listar() {
        return hojeService.listar();
    }
}
