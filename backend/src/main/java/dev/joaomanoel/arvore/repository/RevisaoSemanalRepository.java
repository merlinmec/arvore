package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.RevisaoSemanal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisaoSemanalRepository extends JpaRepository<RevisaoSemanal, UUID> {

    Optional<RevisaoSemanal> findBySemana(LocalDate semana);

    List<RevisaoSemanal> findAllByOrderBySemanaDesc();
}
