package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.FrequenciaSemanal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrequenciaSemanalRepository extends JpaRepository<FrequenciaSemanal, UUID> {

    Optional<FrequenciaSemanal> findByAtividadeIdAndVigenteAteIsNull(UUID atividadeId);

    Optional<FrequenciaSemanal> findByAtividadeIdAndVigenteAte(UUID atividadeId, LocalDate vigenteAte);
}
