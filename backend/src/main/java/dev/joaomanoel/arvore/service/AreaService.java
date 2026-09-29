package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.AreaArvoreDTO;
import dev.joaomanoel.arvore.dto.AreaDTO;
import dev.joaomanoel.arvore.dto.CriarAreaRequest;
import dev.joaomanoel.arvore.dto.EditarAreaRequest;
import dev.joaomanoel.arvore.dto.ProgressoDTO;
import dev.joaomanoel.arvore.entity.Area;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.exception.NaoEncontradoException;
import dev.joaomanoel.arvore.repository.AreaRepository;
import dev.joaomanoel.arvore.repository.AtividadeRepository;
import dev.joaomanoel.arvore.repository.ObjetivoRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AreaService {

    private final AreaRepository areaRepository;
    private final ObjetivoRepository objetivoRepository;
    private final AtividadeRepository atividadeRepository;
    private final ProgressoService progressoService;

    public AreaService(
            AreaRepository areaRepository,
            ObjetivoRepository objetivoRepository,
            AtividadeRepository atividadeRepository,
            ProgressoService progressoService) {
        this.areaRepository = areaRepository;
        this.objetivoRepository = objetivoRepository;
        this.atividadeRepository = atividadeRepository;
        this.progressoService = progressoService;
    }

    public AreaDTO criar(CriarAreaRequest requisicao) {
        Area area = new Area();
        area.setNome(requisicao.nome());
        area.setAreaPaiId(requisicao.areaPaiId());
        area.setDescricaoGeral(requisicao.descricaoGeral());
        area.setCorTema(requisicao.corTema());
        area.setWallpaperUrl(requisicao.wallpaperUrl());
        area.setOrdem((int) areaRepository.countByAreaPaiId(requisicao.areaPaiId()));

        Area salva = areaRepository.save(area);
        return paraDTO(salva);
    }

    private AreaDTO paraDTO(Area area) {
        return new AreaDTO(
                area.getId(),
                area.getAreaPaiId(),
                area.getNome(),
                area.getDescricaoGeral(),
                area.getCorTema(),
                area.getWallpaperUrl(),
                area.getOrdem());
    }

    public AreaDTO editar(UUID id, EditarAreaRequest requisicao) {
        Area area =
                areaRepository
                        .findById(id)
                        .orElseThrow(() -> new NaoEncontradoException("Área não encontrada"));
        area.setNome(requisicao.nome());
        area.setDescricaoGeral(requisicao.descricaoGeral());
        area.setCorTema(requisicao.corTema());
        area.setWallpaperUrl(requisicao.wallpaperUrl());

        Area salva = areaRepository.save(area);
        return paraDTO(salva);
    }

    public void excluir(UUID id) {
        areaRepository.deleteById(id);
    }

    public List<AreaArvoreDTO> listarArvore() {
        List<Area> todas = areaRepository.findAllByOrderByOrdemAsc();

        Map<UUID, List<Area>> filhosPorPai =
                todas.stream()
                        .filter(area -> area.getAreaPaiId() != null)
                        .collect(Collectors.groupingBy(Area::getAreaPaiId));

        return todas.stream()
                .filter(area -> area.getAreaPaiId() == null)
                .map(raiz -> montarNo(raiz, filhosPorPai))
                .toList();
    }

    private AreaArvoreDTO montarNo(Area area, Map<UUID, List<Area>> filhosPorPai) {
        List<AreaArvoreDTO> filhos =
                filhosPorPai.getOrDefault(area.getId(), List.of()).stream()
                        .map(filho -> montarNo(filho, filhosPorPai))
                        .toList();

        return new AreaArvoreDTO(
                area.getId(),
                area.getNome(),
                area.getDescricaoGeral(),
                area.getCorTema(),
                area.getWallpaperUrl(),
                area.getOrdem(),
                calcularProgressoDaArea(area.getId()),
                filhos);
    }

    /** Progresso agregado só dos objetivos diretos da área — não soma o de subáreas. */
    private ProgressoDTO calcularProgressoDaArea(UUID areaId) {
        List<UUID> atividadeIds =
                objetivoRepository.findAllByAreaId(areaId).stream()
                        .flatMap(objetivo -> atividadeRepository.findAllByObjetivoId(objetivo.getId()).stream())
                        .map(Atividade::getId)
                        .toList();
        return progressoService.calcular(atividadeIds);
    }
}
