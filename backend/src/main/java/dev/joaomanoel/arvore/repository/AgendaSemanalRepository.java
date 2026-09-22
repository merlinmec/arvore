package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.AgendaSemanal;
import dev.joaomanoel.arvore.entity.DiaSemana;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendaSemanalRepository extends JpaRepository<AgendaSemanal, UUID> {

    List<AgendaSemanal> findAllByDiaSemana(DiaSemana diaSemana);

    List<AgendaSemanal> findAllByAtividadeId(UUID atividadeId);

    Optional<AgendaSemanal> findByAtividadeIdAndDiaSemana(UUID atividadeId, DiaSemana diaSemana);
}
