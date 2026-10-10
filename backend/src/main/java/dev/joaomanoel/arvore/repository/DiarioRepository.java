package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Diario;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiarioRepository extends JpaRepository<Diario, UUID> {

    Optional<Diario> findByData(LocalDate data);

    List<Diario> findAllByDataBetweenOrderByDataDesc(LocalDate inicio, LocalDate fim);
}
