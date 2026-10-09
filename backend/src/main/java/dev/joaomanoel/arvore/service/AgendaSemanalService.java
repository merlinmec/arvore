package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.entity.AgendaSemanal;
import dev.joaomanoel.arvore.entity.Atividade;
import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.entity.FrequenciaSemanal;
import dev.joaomanoel.arvore.exception.NaoEncontradoException;
import dev.joaomanoel.arvore.repository.AgendaSemanalRepository;
import dev.joaomanoel.arvore.repository.AtividadeRepository;
import dev.joaomanoel.arvore.repository.FrequenciaSemanalRepository;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grade de uma atividade: dias fixos ({@code agenda_semanal}) ou uma meta de N vezes por semana
 * ({@code frequencia_semanal}), nunca os dois ao mesmo tempo. Mudanças valem a partir de hoje
 * (inclusive) e nunca reescrevem o passado: sair de uma regra encerra a linha em vez de apagá-la.
 * Linha criada hoje e desfeita hoje nunca valeu um dia passado, então é apagada.
 */
@Service
public class AgendaSemanalService {

    private final AgendaSemanalRepository agendaSemanalRepository;
    private final FrequenciaSemanalRepository frequenciaSemanalRepository;
    private final AtividadeRepository atividadeRepository;

    public AgendaSemanalService(
            AgendaSemanalRepository agendaSemanalRepository,
            FrequenciaSemanalRepository frequenciaSemanalRepository,
            AtividadeRepository atividadeRepository) {
        this.agendaSemanalRepository = agendaSemanalRepository;
        this.frequenciaSemanalRepository = frequenciaSemanalRepository;
        this.atividadeRepository = atividadeRepository;
    }

    @Transactional
    public void agendar(UUID atividadeId, DiaSemana diaSemana) {
        garantirAtividade(atividadeId);
        LocalDate hoje = Datas.hoje();
        encerrarFrequencia(atividadeId, hoje);
        if (agendaSemanalRepository
                .findByAtividadeIdAndDiaSemanaAndVigenteAteIsNull(atividadeId, diaSemana)
                .isPresent()) {
            return;
        }

        // Desmarcou e remarcou no mesmo dia: reabre o trecho em vez de criar outro colado nele.
        AgendaSemanal agenda =
                agendaSemanalRepository
                        .findByAtividadeIdAndDiaSemanaAndVigenteAte(atividadeId, diaSemana, hoje)
                        .orElseGet(
                                () -> {
                                    AgendaSemanal nova = new AgendaSemanal();
                                    nova.setAtividadeId(atividadeId);
                                    nova.setDiaSemana(diaSemana);
                                    nova.setVigenteDe(hoje);
                                    return nova;
                                });
        agenda.setVigenteAte(null);
        agendaSemanalRepository.save(agenda);
    }

    @Transactional
    public void desagendar(UUID atividadeId, DiaSemana diaSemana) {
        LocalDate hoje = Datas.hoje();
        agendaSemanalRepository
                .findByAtividadeIdAndDiaSemanaAndVigenteAteIsNull(atividadeId, diaSemana)
                .ifPresent(agenda -> encerrarAgenda(agenda, hoje));
    }

    /** Troca a atividade pro modo "N vezes por semana", encerrando os dias fixos em vigor. */
    @Transactional
    public void definirFrequencia(UUID atividadeId, int vezes) {
        garantirAtividade(atividadeId);
        LocalDate hoje = Datas.hoje();
        agendaSemanalRepository
                .findAllByAtividadeIdAndVigenteAteIsNull(atividadeId)
                .forEach(agenda -> encerrarAgenda(agenda, hoje));

        FrequenciaSemanal emVigor =
                frequenciaSemanalRepository.findByAtividadeIdAndVigenteAteIsNull(atividadeId).orElse(null);
        if (emVigor != null) {
            if (emVigor.getVezes() == vezes) {
                return;
            }
            if (emVigor.getVigenteDe().equals(hoje)) {
                emVigor.setVezes((short) vezes);
                frequenciaSemanalRepository.save(emVigor);
                return;
            }
            emVigor.setVigenteAte(hoje);
            frequenciaSemanalRepository.save(emVigor);
        }

        // Voltou no mesmo dia pra meta que tinha encerrado: reabre em vez de criar um trecho colado.
        FrequenciaSemanal frequencia =
                frequenciaSemanalRepository
                        .findByAtividadeIdAndVigenteAte(atividadeId, hoje)
                        .filter(encerrada -> encerrada.getVezes() == vezes)
                        .orElseGet(
                                () -> {
                                    FrequenciaSemanal nova = new FrequenciaSemanal();
                                    nova.setAtividadeId(atividadeId);
                                    nova.setVezes((short) vezes);
                                    nova.setVigenteDe(hoje);
                                    return nova;
                                });
        frequencia.setVigenteAte(null);
        frequenciaSemanalRepository.save(frequencia);
    }

    @Transactional
    public void removerFrequencia(UUID atividadeId) {
        encerrarFrequencia(atividadeId, Datas.hoje());
    }

    private void encerrarAgenda(AgendaSemanal agenda, LocalDate hoje) {
        if (agenda.getVigenteDe().equals(hoje)) {
            agendaSemanalRepository.delete(agenda);
        } else {
            agenda.setVigenteAte(hoje);
            agendaSemanalRepository.save(agenda);
        }
    }

    private void encerrarFrequencia(UUID atividadeId, LocalDate hoje) {
        frequenciaSemanalRepository
                .findByAtividadeIdAndVigenteAteIsNull(atividadeId)
                .ifPresent(
                        frequencia -> {
                            if (frequencia.getVigenteDe().equals(hoje)) {
                                frequenciaSemanalRepository.delete(frequencia);
                            } else {
                                frequencia.setVigenteAte(hoje);
                                frequenciaSemanalRepository.save(frequencia);
                            }
                        });
    }

    /** A atividade existe e aceita grade: hábito negativo vale todo dia, então não tem. */
    private void garantirAtividade(UUID atividadeId) {
        Atividade atividade =
                atividadeRepository
                        .findById(atividadeId)
                        .orElseThrow(() -> new NaoEncontradoException("Atividade não encontrada"));
        if (atividade.isNegativa()) {
            throw new IllegalArgumentException("Hábito negativo vale todo dia e não tem grade");
        }
    }
}
