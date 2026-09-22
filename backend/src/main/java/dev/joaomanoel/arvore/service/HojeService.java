package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.ItemHojeDTO;
import dev.joaomanoel.arvore.entity.AgendaSemanal;
import dev.joaomanoel.arvore.entity.Area;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.Objetivo;
import dev.joaomanoel.arvore.repository.AgendaSemanalRepository;
import dev.joaomanoel.arvore.repository.AreaRepository;
import dev.joaomanoel.arvore.repository.AtividadeRepository;
import dev.joaomanoel.arvore.repository.ConclusaoRepository;
import dev.joaomanoel.arvore.repository.ObjetivoRepository;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HojeService {

    private final AgendaSemanalRepository agendaSemanalRepository;
    private final AtividadeRepository atividadeRepository;
    private final ObjetivoRepository objetivoRepository;
    private final AreaRepository areaRepository;
    private final ConclusaoRepository conclusaoRepository;

    public HojeService(
            AgendaSemanalRepository agendaSemanalRepository,
            AtividadeRepository atividadeRepository,
            ObjetivoRepository objetivoRepository,
            AreaRepository areaRepository,
            ConclusaoRepository conclusaoRepository) {
        this.agendaSemanalRepository = agendaSemanalRepository;
        this.atividadeRepository = atividadeRepository;
        this.objetivoRepository = objetivoRepository;
        this.areaRepository = areaRepository;
        this.conclusaoRepository = conclusaoRepository;
    }

    public List<ItemHojeDTO> listar() {
        LocalDate hoje = Datas.hoje();
        var diaSemana = Datas.diaSemanaDe(hoje);

        return agendaSemanalRepository.findAllByDiaSemana(diaSemana).stream()
                .map(agenda -> montarItem(agenda, hoje))
                .toList();
    }

    private ItemHojeDTO montarItem(AgendaSemanal agenda, LocalDate hoje) {
        Atividade atividade =
                atividadeRepository
                        .findById(agenda.getAtividadeId())
                        .orElseThrow(() -> new IllegalStateException("Atividade não encontrada"));
        Objetivo objetivo =
                objetivoRepository
                        .findById(atividade.getObjetivoId())
                        .orElseThrow(() -> new IllegalStateException("Objetivo não encontrado"));
        Area area =
                areaRepository
                        .findById(objetivo.getAreaId())
                        .orElseThrow(() -> new IllegalStateException("Área não encontrada"));
        boolean concluidoHoje =
                conclusaoRepository.findByAtividadeIdAndData(atividade.getId(), hoje).isPresent();

        return new ItemHojeDTO(
                atividade.getId(),
                atividade.getNome(),
                objetivo.getDescricao(),
                area.getId(),
                area.getNome(),
                area.getCorTema(),
                concluidoHoje);
    }
}
