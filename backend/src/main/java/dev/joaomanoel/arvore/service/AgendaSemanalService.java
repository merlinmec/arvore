package dev.joaomanoel.arvore.service;

import dev.joaomanoel.arvore.entity.AgendaSemanal;
import dev.joaomanoel.arvore.entity.DiaSemana;
import dev.joaomanoel.arvore.repository.AgendaSemanalRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AgendaSemanalService {

    private final AgendaSemanalRepository agendaSemanalRepository;

    public AgendaSemanalService(AgendaSemanalRepository agendaSemanalRepository) {
        this.agendaSemanalRepository = agendaSemanalRepository;
    }

    public void agendar(UUID atividadeId, DiaSemana diaSemana) {
        if (agendaSemanalRepository.findByAtividadeIdAndDiaSemana(atividadeId, diaSemana).isPresent()) {
            return;
        }
        AgendaSemanal agenda = new AgendaSemanal();
        agenda.setAtividadeId(atividadeId);
        agenda.setDiaSemana(diaSemana);
        agendaSemanalRepository.save(agenda);
    }

    public void desagendar(UUID atividadeId, DiaSemana diaSemana) {
        agendaSemanalRepository
                .findByAtividadeIdAndDiaSemana(atividadeId, diaSemana)
                .ifPresent(agendaSemanalRepository::delete);
    }
}
