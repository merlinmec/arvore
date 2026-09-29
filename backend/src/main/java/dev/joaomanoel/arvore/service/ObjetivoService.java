package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.AtividadeDTO;
import dev.joaomanoel.arvore.dto.CriarObjetivoRequest;
import dev.joaomanoel.arvore.dto.EditarObjetivoRequest;
import dev.joaomanoel.arvore.dto.ObjetivoComAtividadesDTO;
import dev.joaomanoel.arvore.dto.ObjetivoDTO;
import dev.joaomanoel.arvore.entity.AgendaSemanal;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.entity.Objetivo;
import dev.joaomanoel.arvore.exception.NaoEncontradoException;
import dev.joaomanoel.arvore.repository.AgendaSemanalRepository;
import dev.joaomanoel.arvore.repository.AtividadeRepository;
import dev.joaomanoel.arvore.repository.ObjetivoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ObjetivoService {

    private final ObjetivoRepository objetivoRepository;
    private final AtividadeRepository atividadeRepository;
    private final AgendaSemanalRepository agendaSemanalRepository;
    private final ProgressoService progressoService;

    public ObjetivoService(
            ObjetivoRepository objetivoRepository,
            AtividadeRepository atividadeRepository,
            AgendaSemanalRepository agendaSemanalRepository,
            ProgressoService progressoService) {
        this.objetivoRepository = objetivoRepository;
        this.atividadeRepository = atividadeRepository;
        this.agendaSemanalRepository = agendaSemanalRepository;
        this.progressoService = progressoService;
    }

    public List<ObjetivoComAtividadesDTO> listarPorArea(UUID areaId) {
        return objetivoRepository.findAllByAreaId(areaId).stream()
                .map(this::montarComAtividades)
                .toList();
    }

    private ObjetivoComAtividadesDTO montarComAtividades(Objetivo objetivo) {
        List<Atividade> atividades = atividadeRepository.findAllByObjetivoId(objetivo.getId());
        List<UUID> atividadeIds = atividades.stream().map(Atividade::getId).toList();

        return new ObjetivoComAtividadesDTO(
                objetivo.getId(),
                objetivo.getAreaId(),
                objetivo.getDescricao(),
                objetivo.getPrazo(),
                objetivo.getStatus(),
                progressoService.calcular(atividadeIds),
                atividades.stream().map(this::paraDTO).toList());
    }

    public ObjetivoDTO editar(UUID id, EditarObjetivoRequest requisicao) {
        Objetivo objetivo =
                objetivoRepository
                        .findById(id)
                        .orElseThrow(() -> new NaoEncontradoException("Objetivo não encontrado"));
        objetivo.setDescricao(requisicao.descricao());
        objetivo.setPrazo(requisicao.prazo());
        objetivo.setStatus(requisicao.status());

        Objetivo salvo = objetivoRepository.save(objetivo);
        return paraDTO(salvo);
    }

    public void excluir(UUID id) {
        objetivoRepository.deleteById(id);
    }

    public ObjetivoDTO criar(UUID areaId, CriarObjetivoRequest requisicao) {
        Objetivo objetivo = new Objetivo();
        objetivo.setAreaId(areaId);
        objetivo.setDescricao(requisicao.descricao());
        objetivo.setPrazo(requisicao.prazo());

        Objetivo salvo = objetivoRepository.save(objetivo);
        return paraDTO(salvo);
    }

    private ObjetivoDTO paraDTO(Objetivo objetivo) {
        return new ObjetivoDTO(
                objetivo.getId(),
                objetivo.getAreaId(),
                objetivo.getDescricao(),
                objetivo.getPrazo(),
                objetivo.getStatus());
    }

    private AtividadeDTO paraDTO(Atividade atividade) {
        List<DiaSemana> diasAgendados =
                agendaSemanalRepository.findAllByAtividadeId(atividade.getId()).stream()
                        .map(AgendaSemanal::getDiaSemana)
                        .toList();
        return new AtividadeDTO(
                atividade.getId(), atividade.getObjetivoId(), atividade.getNome(), diasAgendados);
    }
}
