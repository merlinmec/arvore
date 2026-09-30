package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Conclusao;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConclusaoRepository extends JpaRepository<Conclusao, UUID> {

    Optional<Conclusao> findByAtividadeIdAndData(UUID atividadeId, LocalDate data);

    List<Conclusao> findAllByDataBetween(LocalDate inicio, LocalDate fim);

    List<Conclusao> findAllByAtividadeIdInAndDataBetween(
            List<UUID> atividadeIds, LocalDate inicio, LocalDate fim);

    /** Soma dos valores registrados por objetivo, numa query só; objetivo sem valor não aparece. */
    @Query(
            """
            select new dev.joaomanoel.arvore.repository.AcumuladoPorObjetivo(a.objetivoId, sum(c.valor))
            from Conclusao c join Atividade a on a.id = c.atividadeId
            where a.objetivoId in :objetivoIds and c.valor is not null
            group by a.objetivoId
            """)
    List<AcumuladoPorObjetivo> somarValoresPorObjetivo(@Param("objetivoIds") Collection<UUID> objetivoIds);
}
