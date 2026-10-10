package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.DiaConsistenciaDTO;
import dev.joaomanoel.arvore.dto.DiarioConsistenciaDTO;
import dev.joaomanoel.arvore.dto.DiarioDTO;
import dev.joaomanoel.arvore.dto.SalvarDiarioRequest;
import dev.joaomanoel.arvore.entity.Diario;
import dev.joaomanoel.arvore.repository.DiarioRepository;
import dev.joaomanoel.arvore.util.Datas;
import dev.joaomanoel.arvore.util.DiarioCalculo;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Diário do dia: humor, energia e nota por data, e o cruzamento disso com a consistência. */
@Service
public class DiarioService {

    private final DiarioRepository diarioRepository;
    private final InsightsService insightsService;

    public DiarioService(DiarioRepository diarioRepository, InsightsService insightsService) {
        this.diarioRepository = diarioRepository;
        this.insightsService = insightsService;
    }

    public DiarioDTO obter(LocalDate data) {
        return diarioRepository
                .findByData(data)
                .map(DiarioService::paraDTO)
                .orElseGet(() -> new DiarioDTO(data, null, null, null));
    }

    public List<DiarioDTO> listar(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Início depois do fim");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) >= InsightsService.MAX_DIAS_HISTORICO) {
            throw new IllegalArgumentException(
                    "Período máximo é de " + InsightsService.MAX_DIAS_HISTORICO + " dias");
        }
        return diarioRepository.findAllByDataBetweenOrderByDataDesc(inicio, fim).stream()
                .map(DiarioService::paraDTO)
                .toList();
    }

    /**
     * Grava o registro do dia por inteiro. Sem nenhum campo preenchido, o registro é apagado: o
     * diário não guarda linha vazia.
     */
    @Transactional
    public DiarioDTO salvar(LocalDate data, SalvarDiarioRequest requisicao) {
        if (data.isAfter(Datas.hoje())) {
            throw new IllegalArgumentException("Esse dia ainda não chegou");
        }
        String nota = requisicao.nota() == null || requisicao.nota().isBlank() ? null : requisicao.nota().trim();
        if (requisicao.humor() == null && requisicao.energia() == null && nota == null) {
            apagar(data);
            return new DiarioDTO(data, null, null, null);
        }
        Diario diario =
                diarioRepository
                        .findByData(data)
                        .orElseGet(
                                () -> {
                                    Diario novo = new Diario();
                                    novo.setData(data);
                                    return novo;
                                });
        diario.setHumor(requisicao.humor() == null ? null : requisicao.humor().shortValue());
        diario.setEnergia(requisicao.energia() == null ? null : requisicao.energia().shortValue());
        diario.setNota(nota);
        return paraDTO(diarioRepository.saveAndFlush(diario));
    }

    @Transactional
    public void apagar(LocalDate data) {
        diarioRepository.findByData(data).ifPresent(diarioRepository::delete);
    }

    /** Taxa de conclusão por nível de humor e de energia nos últimos {@code dias} dias. */
    public DiarioConsistenciaDTO consistencia(int dias, UUID areaId) {
        if (dias < 1 || dias > InsightsService.MAX_DIAS_RESUMO) {
            throw new IllegalArgumentException(
                    "Período deve ter entre 1 e " + InsightsService.MAX_DIAS_RESUMO + " dias");
        }
        LocalDate hoje = Datas.hoje();
        LocalDate inicio = hoje.minusDays(dias - 1L);
        List<DiaConsistenciaDTO> consistencia = insightsService.historico(inicio, hoje, areaId);
        List<Diario> registros = diarioRepository.findAllByDataBetweenOrderByDataDesc(inicio, hoje);

        return new DiarioConsistenciaDTO(
                registros.size(),
                DiarioCalculo.porNivel(nivelPorData(registros, Diario::getHumor), consistencia),
                DiarioCalculo.porNivel(nivelPorData(registros, Diario::getEnergia), consistencia));
    }

    private static Map<LocalDate, Integer> nivelPorData(List<Diario> registros, Function<Diario, Short> nivel) {
        Map<LocalDate, Integer> mapa = new HashMap<>();
        for (Diario registro : registros) {
            Short valor = nivel.apply(registro);
            if (valor != null) {
                mapa.put(registro.getData(), valor.intValue());
            }
        }
        return mapa;
    }

    private static DiarioDTO paraDTO(Diario diario) {
        return new DiarioDTO(
                diario.getData(),
                diario.getHumor() == null ? null : diario.getHumor().intValue(),
                diario.getEnergia() == null ? null : diario.getEnergia().intValue(),
                diario.getNota());
    }
}
