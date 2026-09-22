package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.dto.AtividadeDTO;
import dev.joaomanoel.arvore.dto.CriarAtividadeRequest;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.repository.AtividadeRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AtividadeService {

    private final AtividadeRepository atividadeRepository;

    public AtividadeService(AtividadeRepository atividadeRepository) {
        this.atividadeRepository = atividadeRepository;
    }

    public void excluir(UUID id) {
        atividadeRepository.deleteById(id);
    }

    public AtividadeDTO criar(UUID objetivoId, CriarAtividadeRequest requisicao) {
        Atividade atividade = new Atividade();
        atividade.setObjetivoId(objetivoId);
        atividade.setNome(requisicao.nome());

        Atividade salva = atividadeRepository.save(atividade);
        return paraDTO(salva);
    }

    private AtividadeDTO paraDTO(Atividade atividade) {
        return new AtividadeDTO(
                atividade.getId(), atividade.getObjetivoId(), atividade.getNome(), List.of());
    }
}
