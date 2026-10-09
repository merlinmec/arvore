package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.Conclusao;
import dev.joaomanoel.arvore.exception.NaoEncontradoException;
import dev.joaomanoel.arvore.repository.AtividadeRepository;
import dev.joaomanoel.arvore.repository.ConclusaoRepository;
import dev.joaomanoel.arvore.util.Datas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConclusaoService {

    private final ConclusaoRepository conclusaoRepository;
    private final AtividadeRepository atividadeRepository;

    public ConclusaoService(
            ConclusaoRepository conclusaoRepository, AtividadeRepository atividadeRepository) {
        this.conclusaoRepository = conclusaoRepository;
        this.atividadeRepository = atividadeRepository;
    }

    /**
     * Marca a atividade como feita em {@code data}; aceita dias passados (registro retroativo), nunca
     * futuros. Com {@code valor} ("5" km), marcar de novo no mesmo dia corrige o valor registrado.
     */
    public void marcar(UUID atividadeId, LocalDate data, BigDecimal valor) {
        Atividade atividade = validar(atividadeId, data);
        if (valor != null && atividade.isNegativa()) {
            throw new IllegalArgumentException("Recaída de hábito negativo não registra valor");
        }
        if (valor != null && valor.signum() < 0) {
            throw new IllegalArgumentException("O valor não pode ser negativo");
        }
        Conclusao existente = conclusaoRepository.findByAtividadeIdAndData(atividadeId, data).orElse(null);
        if (existente != null) {
            if (valor != null) {
                existente.setValor(valor);
                conclusaoRepository.save(existente);
            }
            return;
        }
        Conclusao conclusao = new Conclusao();
        conclusao.setAtividadeId(atividadeId);
        conclusao.setData(data);
        conclusao.setValor(valor);
        conclusaoRepository.save(conclusao);
    }

    public void desmarcar(UUID atividadeId, LocalDate data) {
        validar(atividadeId, data);
        conclusaoRepository
                .findByAtividadeIdAndData(atividadeId, data)
                .ifPresent(conclusaoRepository::delete);
    }

    private Atividade validar(UUID atividadeId, LocalDate data) {
        if (data.isAfter(Datas.hoje())) {
            throw new IllegalArgumentException("Não dá pra concluir uma atividade num dia que ainda não chegou");
        }
        return atividadeRepository
                .findById(atividadeId)
                .orElseThrow(() -> new NaoEncontradoException("Atividade não encontrada"));
    }
}
