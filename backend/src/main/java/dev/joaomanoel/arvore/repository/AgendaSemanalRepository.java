package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.AgendaSemanal;
import dev.joaomanoel.arvore.entity.DiaSemana;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendaSemanalRepository extends JpaRepository<AgendaSemanal, UUID> {

    List<AgendaSemanal> findAllByAtividadeIdAndVigenteAteIsNull(UUID atividadeId);

    Optional<AgendaSemanal> findByAtividadeIdAndDiaSemanaAndVigenteAteIsNull(
            UUID atividadeId, DiaSemana diaSemana);

    Optional<AgendaSemanal> findByAtividadeIdAndDiaSemanaAndVigenteAte(
            UUID atividadeId, DiaSemana diaSemana, LocalDate vigenteAte);
}
