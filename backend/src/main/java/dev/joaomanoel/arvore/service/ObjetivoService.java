package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.AtividadeDTO;
import dev.joaomanoel.arvore.dto.CriarObjetivoRequest;
import dev.joaomanoel.arvore.dto.EditarObjetivoRequest;
import dev.joaomanoel.arvore.dto.MetaDTO;
import dev.joaomanoel.arvore.dto.ObjetivoComAtividadesDTO;
import dev.joaomanoel.arvore.dto.ObjetivoDTO;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.Objetivo;
import dev.joaomanoel.arvore.exception.NaoEncontradoException;
import dev.joaomanoel.arvore.repository.AcumuladoPorObjetivo;
import dev.joaomanoel.arvore.repository.AreaRepository;
import dev.joaomanoel.arvore.repository.ConclusaoRepository;
import dev.joaomanoel.arvore.repository.ObjetivoRepository;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import dev.joaomanoel.arvore.util.Datas;
import dev.joaomanoel.arvore.util.MetaCalculo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ObjetivoService {

    private final ObjetivoRepository objetivoRepository;
    private final AreaRepository areaRepository;
    private final ConclusaoRepository conclusaoRepository;
    private final CatalogoService catalogoService;
    private final ProgressoService progressoService;

    public ObjetivoService(
            ObjetivoRepository objetivoRepository,
            AreaRepository areaRepository,
            ConclusaoRepository conclusaoRepository,
            CatalogoService catalogoService,
            ProgressoService progressoService) {
        this.objetivoRepository = objetivoRepository;
        this.areaRepository = areaRepository;
        this.conclusaoRepository = conclusaoRepository;
        this.catalogoService = catalogoService;
        this.progressoService = progressoService;
    }

    public List<ObjetivoComAtividadesDTO> listarPorArea(UUID areaId) {
        Catalogo catalogo = catalogoService.carregar();
        ProgressoService.Semana semana = progressoService.semanaAtual();
        List<Objetivo> objetivos = catalogo.objetivosDe(areaId);
        Map<UUID, BigDecimal> acumulados = acumulados(objetivos);
        LocalDate hoje = Datas.hoje();

        return objetivos.stream()
                .map(
                        objetivo -> {
                            BigDecimal acumulado = acumulados.getOrDefault(objetivo.getId(), BigDecimal.ZERO);
                            return montarComAtividades(objetivo, catalogo, semana, meta(objetivo, acumulado, hoje));
                        })
                .toList();
    }

    /** Uma query só pra somar os valores de todos os objetivos com meta da lista. */
    private Map<UUID, BigDecimal> acumulados(List<Objetivo> objetivos) {
        List<UUID> comMeta =
                objetivos.stream()
                        .filter(objetivo -> objetivo.getMetaValor() != null)
                        .map(Objetivo::getId)
                        .toList();
        if (comMeta.isEmpty()) {
            return Map.of();
        }
        return conclusaoRepository.somarValoresPorObjetivo(comMeta).stream()
                .collect(Collectors.toMap(AcumuladoPorObjetivo::objetivoId, AcumuladoPorObjetivo::total));
    }

    private static MetaDTO meta(Objetivo objetivo, BigDecimal acumulado, LocalDate hoje) {
        if (objetivo.getMetaValor() == null) {
            return null;
        }
        return MetaCalculo.calcular(
                objetivo.getMetaValor(),
                objetivo.getMetaUnidade(),
                acumulado,
                Datas.dataDe(objetivo.getCriadoEm()),
                objetivo.getPrazo(),
                hoje);
    }

    /** O progresso é o do próprio objetivo, qualquer que seja o status: o card mostra pausados também. */
    private ObjetivoComAtividadesDTO montarComAtividades(
            Objetivo objetivo, Catalogo catalogo, ProgressoService.Semana semana, MetaDTO meta) {
        return new ObjetivoComAtividadesDTO(
                objetivo.getId(),
                objetivo.getAreaId(),
                objetivo.getDescricao(),
                objetivo.getPrazo(),
                objetivo.getStatus(),
                semana.progresso(catalogo.rotinasDe(objetivo)),
                meta,
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
        aplicarMeta(objetivo, requisicao.metaValor(), requisicao.metaUnidade());

        Objetivo salvo = objetivoRepository.save(objetivo);
        return paraDTO(salvo);
    }

    public void excluir(UUID id) {
        objetivoRepository.deleteById(id);
    }

    public ObjetivoDTO criar(UUID areaId, CriarObjetivoRequest requisicao) {
        if (!areaRepository.existsById(areaId)) {
            throw new NaoEncontradoException("Área não encontrada");
        }
        Objetivo objetivo = new Objetivo();
        objetivo.setAreaId(areaId);
        objetivo.setDescricao(requisicao.descricao());
        objetivo.setPrazo(requisicao.prazo());
        aplicarMeta(objetivo, requisicao.metaValor(), requisicao.metaUnidade());

        Objetivo salvo = objetivoRepository.save(objetivo);
        return paraDTO(salvo);
    }

    /** Valor e unidade vêm juntos, ou nenhum dos dois (objetivo sem meta). */
    private static void aplicarMeta(Objetivo objetivo, BigDecimal valor, String unidade) {
        String unidadeLimpa = unidade == null || unidade.isBlank() ? null : unidade.trim();
        if ((valor == null) != (unidadeLimpa == null)) {
            throw new IllegalArgumentException("A meta precisa de valor e unidade juntos");
        }
        objetivo.setMetaValor(valor);
        objetivo.setMetaUnidade(unidadeLimpa);
    }

    private ObjetivoDTO paraDTO(Objetivo objetivo) {
        return new ObjetivoDTO(
                objetivo.getId(),
                objetivo.getAreaId(),
                objetivo.getDescricao(),
                objetivo.getPrazo(),
                objetivo.getStatus(),
                objetivo.getMetaValor(),
                objetivo.getMetaUnidade());
    }

    private AtividadeDTO paraDTO(Atividade atividade, Rotina rotina) {
        return new AtividadeDTO(
                atividade.getId(),
                atividade.getObjetivoId(),
                atividade.getNome(),
                rotina.diasAtuais(),
                rotina.vezesAtuais(),
                atividade.isNegativa());
    }
}
