package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.AtividadeDTO;
import dev.joaomanoel.arvore.dto.CriarObjetivoRequest;
import dev.joaomanoel.arvore.dto.EditarObjetivoRequest;
import dev.joaomanoel.arvore.dto.ObjetivoComAtividadesDTO;
import dev.joaomanoel.arvore.dto.ObjetivoDTO;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.Objetivo;
import dev.joaomanoel.arvore.exception.NaoEncontradoException;
import dev.joaomanoel.arvore.repository.ObjetivoRepository;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ObjetivoService {

    private final ObjetivoRepository objetivoRepository;
    private final CatalogoService catalogoService;
    private final ProgressoService progressoService;

    public ObjetivoService(
            ObjetivoRepository objetivoRepository,
            CatalogoService catalogoService,
            ProgressoService progressoService) {
        this.objetivoRepository = objetivoRepository;
        this.catalogoService = catalogoService;
        this.progressoService = progressoService;
    }

    public List<ObjetivoComAtividadesDTO> listarPorArea(UUID areaId) {
        Catalogo catalogo = catalogoService.carregar();
        ProgressoService.Semana semana = progressoService.semanaAtual();
        return catalogo.objetivosDe(areaId).stream()
                .map(objetivo -> montarComAtividades(objetivo, catalogo, semana))
                .toList();
    }

    /** O progresso é o do próprio objetivo, qualquer que seja o status: o card mostra pausados também. */
    private ObjetivoComAtividadesDTO montarComAtividades(
            Objetivo objetivo, Catalogo catalogo, ProgressoService.Semana semana) {
        return new ObjetivoComAtividadesDTO(
                objetivo.getId(),
                objetivo.getAreaId(),
                objetivo.getDescricao(),
                objetivo.getPrazo(),
                objetivo.getStatus(),
                semana.progresso(catalogo.rotinasDe(objetivo)),
                catalogo.atividadesDe(objetivo.getId()).stream()
                        .map(atividade -> paraDTO(atividade, catalogo.rotina(atividade.getId())))
                        .toList());
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

    private AtividadeDTO paraDTO(Atividade atividade, Rotina rotina) {
        return new AtividadeDTO(
                atividade.getId(), atividade.getObjetivoId(), atividade.getNome(), rotina.diasAtuais());
    }
}
