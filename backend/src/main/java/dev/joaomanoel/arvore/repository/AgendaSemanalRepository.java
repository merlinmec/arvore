package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.AgendaSemanal;
import dev.joaomanoel.arvore.entity.DiaSemana;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendaSemanalRepository extends JpaRepository<AgendaSemanal, UUID> {

    Optional<AgendaSemanal> findByAtividadeIdAndDiaSemana(UUID atividadeId, DiaSemana diaSemana);
}
