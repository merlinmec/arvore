package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.CriarNotaRequest;
import dev.joaomanoel.arvore.dto.NotaComAreaDTO;
import dev.joaomanoel.arvore.dto.NotaDTO;
import dev.joaomanoel.arvore.entity.Area;
import dev.joaomanoel.arvore.entity.Nota;
import dev.joaomanoel.arvore.entity.Tag;
import dev.joaomanoel.arvore.repository.AreaRepository;
import dev.joaomanoel.arvore.repository.NotaRepository;
import dev.joaomanoel.arvore.repository.TagRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotaService {

    private final NotaRepository notaRepository;
    private final TagRepository tagRepository;
    private final AreaRepository areaRepository;

    public NotaService(
            NotaRepository notaRepository, TagRepository tagRepository, AreaRepository areaRepository) {
        this.notaRepository = notaRepository;
        this.tagRepository = tagRepository;
        this.areaRepository = areaRepository;
    }

    @Transactional(readOnly = true)
    public List<NotaDTO> listarPorArea(UUID areaId) {
        return notaRepository.findAllByAreaIdOrderByAtualizadoEmDesc(areaId).stream()
                .map(this::paraDTO)
                .toList();
    }

    @Transactional
    public NotaDTO editar(UUID id, CriarNotaRequest requisicao) {
        Nota nota =
                notaRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Nota não encontrada"));
        nota.setTitulo(requisicao.titulo());
        nota.setCorpo(requisicao.corpo() != null ? requisicao.corpo() : "");
        nota.setTags(resolverTags(requisicao.tags()));

        Nota salva = notaRepository.save(nota);
        return paraDTO(salva);
    }

    public void excluir(UUID id) {
        notaRepository.deleteById(id);
    }

    @Transactional
    public NotaDTO criar(UUID areaId, CriarNotaRequest requisicao) {
        Nota nota = new Nota();
        nota.setAreaId(areaId);
        nota.setTitulo(requisicao.titulo());
        nota.setCorpo(requisicao.corpo() != null ? requisicao.corpo() : "");
        nota.setTags(resolverTags(requisicao.tags()));

        Nota salva = notaRepository.save(nota);
        return paraDTO(salva);
    }

    @Transactional(readOnly = true)
    public List<NotaComAreaDTO> buscarPorTag(String tagNome) {
        String normalizado = normalizarTag(tagNome);

        return notaRepository.findAll().stream()
                .filter(nota -> nota.getTags().stream().anyMatch(tag -> tag.getNome().equals(normalizado)))
                .map(this::paraComAreaDTO)
                .toList();
    }

    private Set<Tag> resolverTags(List<String> nomes) {
        if (nomes == null) {
            return new HashSet<>();
        }
        return nomes.stream()
                .map(this::normalizarTag)
                .filter(nome -> !nome.isBlank())
                .distinct()
                .map(this::buscarOuCriarTag)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String normalizarTag(String nome) {
        return nome.trim().toLowerCase();
    }

    private Tag buscarOuCriarTag(String nome) {
        return tagRepository
                .findByNome(nome)
                .orElseGet(
                        () -> {
                            Tag tag = new Tag();
                            tag.setNome(nome);
                            return tagRepository.save(tag);
                        });
    }

    private NotaDTO paraDTO(Nota nota) {
        return new NotaDTO(
                nota.getId(),
                nota.getAreaId(),
                nota.getTitulo(),
                nota.getCorpo(),
                tagsOrdenadas(nota),
                nota.getCriadoEm(),
                nota.getAtualizadoEm());
    }

    private NotaComAreaDTO paraComAreaDTO(Nota nota) {
        Area area =
                areaRepository
                        .findById(nota.getAreaId())
                        .orElseThrow(() -> new IllegalStateException("Área não encontrada"));
        return new NotaComAreaDTO(
                nota.getId(),
                nota.getAreaId(),
                area.getNome(),
                nota.getTitulo(),
                nota.getCorpo(),
                tagsOrdenadas(nota),
                nota.getAtualizadoEm());
    }

    private List<String> tagsOrdenadas(Nota nota) {
        return nota.getTags().stream().map(Tag::getNome).sorted().toList();
    }
}
