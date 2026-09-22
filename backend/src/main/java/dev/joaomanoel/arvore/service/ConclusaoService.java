package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.entity.Conclusao;
import dev.joaomanoel.arvore.repository.ConclusaoRepository;
import dev.joaomanoel.arvore.util.Datas;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConclusaoService {

    private final ConclusaoRepository conclusaoRepository;

    public ConclusaoService(ConclusaoRepository conclusaoRepository) {
        this.conclusaoRepository = conclusaoRepository;
    }

    public void marcarHoje(UUID atividadeId) {
        var hoje = Datas.hoje();
        if (conclusaoRepository.findByAtividadeIdAndData(atividadeId, hoje).isPresent()) {
            return;
        }
        Conclusao conclusao = new Conclusao();
        conclusao.setAtividadeId(atividadeId);
        conclusao.setData(hoje);
        conclusaoRepository.save(conclusao);
    }

    public void desmarcarHoje(UUID atividadeId) {
        conclusaoRepository
                .findByAtividadeIdAndData(atividadeId, Datas.hoje())
                .ifPresent(conclusaoRepository::delete);
    }
}
