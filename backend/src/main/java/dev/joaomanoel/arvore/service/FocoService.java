package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.SessaoFocoDTO;
import dev.joaomanoel.arvore.dto.TempoFocoDTO;
import dev.joaomanoel.arvore.entity.Area;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.SessaoFoco;
import dev.joaomanoel.arvore.exception.NaoEncontradoException;
import dev.joaomanoel.arvore.repository.AtividadeRepository;
import dev.joaomanoel.arvore.repository.SessaoFocoRepository;
import dev.joaomanoel.arvore.util.Datas;
import dev.joaomanoel.arvore.util.TempoCalculo;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Temporizador de foco: uma sessão por vez, e o tempo somado por dia, área e atividade. */
@Service
public class FocoService {

    private final SessaoFocoRepository sessaoRepository;
    private final AtividadeRepository atividadeRepository;
    private final CatalogoService catalogoService;

    public FocoService(
            SessaoFocoRepository sessaoRepository,
            AtividadeRepository atividadeRepository,
            CatalogoService catalogoService) {
        this.sessaoRepository = sessaoRepository;
        this.atividadeRepository = atividadeRepository;
        this.catalogoService = catalogoService;
    }

    /** A sessão em andamento, se houver. Uma esquecida aberta além do limite é encerrada ali. */
    @Transactional
    public Optional<SessaoFocoDTO> atual() {
        return emAndamento().map(sessao -> paraDTO(sessao, catalogoService.carregar(), Instant.now()));
    }

    /** Começa a contar na atividade; se outra estava contando, ela é encerrada agora. */
    @Transactional
    public SessaoFocoDTO iniciar(UUID atividadeId) {
        Atividade atividade =
                atividadeRepository
                        .findById(atividadeId)
                        .orElseThrow(() -> new NaoEncontradoException("Atividade não encontrada"));
        if (atividade.isNegativa()) {
            throw new IllegalArgumentException("Hábito negativo não tem tempo de foco");
        }
        Instant agora = Instant.now();
        Optional<SessaoFoco> aberta = emAndamento();
        if (aberta.isPresent() && aberta.get().getAtividadeId().equals(atividadeId)) {
            return paraDTO(aberta.get(), catalogoService.carregar(), agora);
        }
        aberta.ifPresent(sessao -> encerrar(sessao, agora));

        SessaoFoco nova = new SessaoFoco();
        nova.setAtividadeId(atividadeId);
        nova.setInicio(agora);
        return paraDTO(sessaoRepository.saveAndFlush(nova), catalogoService.carregar(), agora);
    }

    @Transactional
    public SessaoFocoDTO parar() {
        SessaoFoco sessao =
                emAndamento().orElseThrow(() -> new NaoEncontradoException("Nenhuma sessão de foco em andamento"));
        Instant agora = Instant.now();
        encerrar(sessao, agora);
        return paraDTO(sessao, catalogoService.carregar(), agora);
    }

    /** Sessões com algum trecho entre os dois dias (inclusive), da mais recente pra mais antiga. */
    public List<SessaoFocoDTO> listar(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Início depois do fim");
        }
        Catalogo catalogo = catalogoService.carregar();
        Instant agora = Instant.now();
        return sessaoRepository.sobrepostas(Datas.inicioDoDia(inicio), Datas.inicioDoDia(fim.plusDays(1))).stream()
                .map(sessao -> paraDTO(sessao, catalogo, agora))
                .toList();
    }

    @Transactional
    public void apagar(UUID id) {
        if (!sessaoRepository.existsById(id)) {
            throw new NaoEncontradoException("Sessão não encontrada");
        }
        sessaoRepository.deleteById(id);
    }

    /** Tempo de foco dos últimos {@code dias} dias, opcionalmente só numa área e nas subáreas. */
    public TempoFocoDTO tempo(int dias, UUID areaId) {
        if (dias < 1 || dias > InsightsService.MAX_DIAS_RESUMO) {
            throw new IllegalArgumentException(
                    "Período deve ter entre 1 e " + InsightsService.MAX_DIAS_RESUMO + " dias");
        }
        Catalogo catalogo = catalogoService.carregar();
        if (areaId != null && !catalogo.existeArea(areaId)) {
            throw new NaoEncontradoException("Área não encontrada");
        }
        Set<UUID> areasPermitidas = areaId == null ? null : catalogo.comDescendentes(areaId);
        LocalDate hoje = Datas.hoje();
        LocalDate inicio = hoje.minusDays(dias - 1L);
        Instant de = Datas.inicioDoDia(inicio);
        Instant ate = Datas.inicioDoDia(hoje.plusDays(1));
        Instant agora = Instant.now();

        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        for (LocalDate dia = inicio; !dia.isAfter(hoje); dia = dia.plusDays(1)) {
            porDia.put(dia, 0L);
        }
        Map<UUID, Long> porRamo = new LinkedHashMap<>();
        Map<UUID, long[]> porAtividade = new LinkedHashMap<>();
        long total = 0;
        int sessoes = 0;

        for (SessaoFoco sessao : sessaoRepository.sobrepostas(de, ate)) {
            Atividade atividade = catalogo.atividade(sessao.getAtividadeId());
            Area area = catalogo.areaDe(atividade);
            if (areasPermitidas != null && !areasPermitidas.contains(area.getId())) {
                continue;
            }
            Instant fimEfetivo = TempoCalculo.fimEfetivo(sessao.getInicio(), sessao.getFim(), agora);
            Map<LocalDate, Long> trechos =
                    TempoCalculo.segundosPorDia(sessao.getInicio(), fimEfetivo, de, ate, Datas.fuso());
            long segundos = trechos.values().stream().mapToLong(Long::longValue).sum();
            if (segundos == 0) {
                continue;
            }
            trechos.forEach((dia, valor) -> porDia.merge(dia, valor, Long::sum));
            porRamo.merge(catalogo.ramoDe(area.getId(), areaId).getId(), segundos, Long::sum);
            long[] daAtividade = porAtividade.computeIfAbsent(atividade.getId(), chave -> new long[2]);
            daAtividade[0] += segundos;
            daAtividade[1]++;
            total += segundos;
            sessoes++;
        }

        List<TempoFocoDTO.DiaDTO> serie = new ArrayList<>();
        porDia.forEach((dia, segundos) -> serie.add(new TempoFocoDTO.DiaDTO(dia, TempoCalculo.minutos(segundos))));
        List<TempoFocoDTO.AreaDTO> areas =
                porRamo.entrySet().stream()
                        .map(entrada -> {
                            Area ramo = catalogo.area(entrada.getKey());
                            return new TempoFocoDTO.AreaDTO(
                                    ramo.getId(), ramo.getNome(), ramo.getCorTema(), TempoCalculo.minutos(entrada.getValue()));
                        })
                        .sorted(Comparator.comparingInt(TempoFocoDTO.AreaDTO::minutos).reversed())
                        .toList();
        List<TempoFocoDTO.AtividadeDTO> atividades =
                porAtividade.entrySet().stream()
                        .map(entrada -> {
                            Atividade atividade = catalogo.atividade(entrada.getKey());
                            Area area = catalogo.areaDe(atividade);
                            return new TempoFocoDTO.AtividadeDTO(
                                    atividade.getId(),
                                    atividade.getNome(),
                                    area.getNome(),
                                    area.getCorTema(),
                                    TempoCalculo.minutos(entrada.getValue()[0]),
                                    (int) entrada.getValue()[1]);
                        })
                        .sorted(Comparator.comparingInt(TempoFocoDTO.AtividadeDTO::minutos).reversed())
                        .toList();

        return new TempoFocoDTO(inicio, hoje, TempoCalculo.minutos(total), sessoes, serie, areas, atividades);
    }

    private Optional<SessaoFoco> emAndamento() {
        Optional<SessaoFoco> aberta = sessaoRepository.findByFimIsNull();
        Instant agora = Instant.now();
        if (aberta.isPresent() && Duration.between(aberta.get().getInicio(), agora).compareTo(TempoCalculo.SESSAO_MAXIMA) > 0) {
            encerrar(aberta.get(), agora);
            return Optional.empty();
        }
        return aberta;
    }

    /** Fecha a sessão; se durou menos de um minuto, foi clique sem querer e é descartada. */
    private void encerrar(SessaoFoco sessao, Instant agora) {
        sessao.setFim(TempoCalculo.fimEfetivo(sessao.getInicio(), null, agora));
        if (Duration.between(sessao.getInicio(), sessao.getFim()).compareTo(TempoCalculo.SESSAO_MINIMA) < 0) {
            sessaoRepository.delete(sessao);
            sessaoRepository.flush();
        } else {
            sessaoRepository.saveAndFlush(sessao);
        }
    }

    private static SessaoFocoDTO paraDTO(SessaoFoco sessao, Catalogo catalogo, Instant agora) {
        Atividade atividade = catalogo.atividade(sessao.getAtividadeId());
        Area area = catalogo.areaDe(atividade);
        Instant fim = TempoCalculo.fimEfetivo(sessao.getInicio(), sessao.getFim(), agora);
        return new SessaoFocoDTO(
                sessao.getId(),
                atividade.getId(),
                atividade.getNome(),
                area.getId(),
                area.getNome(),
                area.getCorTema(),
                sessao.getInicio(),
                sessao.getFim(),
                TempoCalculo.minutos(Duration.between(sessao.getInicio(), fim).toSeconds()));
    }
}
