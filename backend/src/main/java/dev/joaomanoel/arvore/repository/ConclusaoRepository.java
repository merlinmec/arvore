package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Conclusao;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConclusaoRepository extends JpaRepository<Conclusao, UUID> {

    Optional<Conclusao> findByAtividadeIdAndData(UUID atividadeId, LocalDate data);

    List<Conclusao> findAllByDataBetween(LocalDate inicio, LocalDate fim);

    List<Conclusao> findAllByAtividadeIdInAndDataBetween(
            List<UUID> atividadeIds, LocalDate inicio, LocalDate fim);
}
