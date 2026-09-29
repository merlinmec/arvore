package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.entity.AgendaSemanal;
import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.repository.AgendaSemanalRepository;
import dev.joaomanoel.arvore.util.Datas;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mudanças na grade valem a partir de hoje (inclusive) e nunca reescrevem o passado: desmarcar um
 * dia encerra a linha em vigor em vez de apagá-la.
 */
@Service
public class AgendaSemanalService {

    private final AgendaSemanalRepository agendaSemanalRepository;

    public AgendaSemanalService(AgendaSemanalRepository agendaSemanalRepository) {
        this.agendaSemanalRepository = agendaSemanalRepository;
    }

    @Transactional
    public void agendar(UUID atividadeId, DiaSemana diaSemana) {
        if (agendaSemanalRepository
                .findByAtividadeIdAndDiaSemanaAndVigenteAteIsNull(atividadeId, diaSemana)
                .isPresent()) {
            return;
        }
        LocalDate hoje = Datas.hoje();

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
                .ifPresent(
                        agenda -> {
                            // Linha criada hoje nunca chegou a valer um dia passado: some sem deixar rastro.
                            if (agenda.getVigenteDe().equals(hoje)) {
                                agendaSemanalRepository.delete(agenda);
                            } else {
                                agenda.setVigenteAte(hoje);
                                agendaSemanalRepository.save(agenda);
                            }
                        });
    }
}
