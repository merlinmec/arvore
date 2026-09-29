package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.AreaArvoreDTO;
import dev.joaomanoel.arvore.dto.AreaDTO;
import dev.joaomanoel.arvore.dto.CriarAreaRequest;
import dev.joaomanoel.arvore.dto.EditarAreaRequest;
import dev.joaomanoel.arvore.entity.Area;
import dev.joaomanoel.arvore.entity.StatusObjetivo;
import dev.joaomanoel.arvore.exception.NaoEncontradoException;
import dev.joaomanoel.arvore.repository.AreaRepository;
import dev.joaomanoel.arvore.util.Consistencia.Rotina;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AreaService {

    private final AreaRepository areaRepository;
    private final CatalogoService catalogoService;
    private final ProgressoService progressoService;

    public AreaService(
            AreaRepository areaRepository,
            CatalogoService catalogoService,
            ProgressoService progressoService) {
        this.areaRepository = areaRepository;
        this.catalogoService = catalogoService;
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
        Catalogo catalogo = catalogoService.carregar();
        ProgressoService.Semana semana = progressoService.semanaAtual();
        return catalogo.raizes().stream().map(raiz -> montarNo(raiz, catalogo, semana)).toList();
    }

    private AreaArvoreDTO montarNo(Area area, Catalogo catalogo, ProgressoService.Semana semana) {
        List<AreaArvoreDTO> filhos =
                catalogo.filhosDe(area.getId()).stream()
                        .map(filho -> montarNo(filho, catalogo, semana))
                        .toList();

        return new AreaArvoreDTO(
                area.getId(),
                area.getNome(),
                area.getDescricaoGeral(),
                area.getCorTema(),
                area.getWallpaperUrl(),
                area.getOrdem(),
                semana.progresso(rotinasEmCurso(area, catalogo)),
                filhos);
    }

    /**
     * Rotinas dos objetivos ATIVOS diretos da área: não soma subáreas, e objetivo pausado ou
     * arquivado não conta como plano (mesma regra de Hoje e do Painel).
     */
    private List<Rotina> rotinasEmCurso(Area area, Catalogo catalogo) {
        return catalogo.objetivosDe(area.getId()).stream()
                .filter(objetivo -> objetivo.getStatus() == StatusObjetivo.ATIVO)
                .flatMap(objetivo -> catalogo.rotinasDe(objetivo).stream())
                .toList();
    }
}
