package dev.joaomanoel.arvore.controller;

import dev.joaomanoel.arvore.dto.NotaComAreaDTO;
import dev.joaomanoel.arvore.repository.TagRepository;
import dev.joaomanoel.arvore.service.NotaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final NotaService notaService;
    private final TagRepository tagRepository;

    public TagController(NotaService notaService, TagRepository tagRepository) {
        this.notaService = notaService;
        this.tagRepository = tagRepository;
    }

    @GetMapping
    public List<String> listarTodas() {
        return tagRepository.nomesEmUso();
    }

    @GetMapping("/{nome}/notas")
    public List<NotaComAreaDTO> notasComTag(@PathVariable String nome) {
        return notaService.buscarPorTag(nome);
    }
}
