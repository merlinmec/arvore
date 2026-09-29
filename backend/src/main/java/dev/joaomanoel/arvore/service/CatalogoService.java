package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.repository.AgendaSemanalRepository;
import dev.joaomanoel.arvore.repository.AreaRepository;
import dev.joaomanoel.arvore.repository.AtividadeRepository;
import dev.joaomanoel.arvore.repository.ObjetivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {

    private final AreaRepository areaRepository;
    private final ObjetivoRepository objetivoRepository;
    private final AtividadeRepository atividadeRepository;
    private final AgendaSemanalRepository agendaSemanalRepository;

    public CatalogoService(
            AreaRepository areaRepository,
            ObjetivoRepository objetivoRepository,
            AtividadeRepository atividadeRepository,
            AgendaSemanalRepository agendaSemanalRepository) {
        this.areaRepository = areaRepository;
        this.objetivoRepository = objetivoRepository;
        this.atividadeRepository = atividadeRepository;
        this.agendaSemanalRepository = agendaSemanalRepository;
    }

    @Transactional(readOnly = true)
    public Catalogo carregar() {
        return new Catalogo(
                areaRepository.findAllByOrderByOrdemAsc(),
                objetivoRepository.findAll(),
                atividadeRepository.findAll(),
                agendaSemanalRepository.findAll());
    }
}
