package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Nota;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotaRepository extends JpaRepository<Nota, UUID> {

    List<Nota> findAllByAreaIdOrderByAtualizadoEmDesc(UUID areaId);

    /**
     * Filtra pela tag numa subquery (em vez de um join no select) pra que as tags carregadas junto
     * sejam todas as da nota, não só a buscada.
     */
    @EntityGraph(attributePaths = "tags")
    @Query(
            """
            select n from Nota n
            where exists (select 1 from Nota m join m.tags t where m = n and t.nome = :tag)
            order by n.atualizadoEm desc
            """)
    List<Nota> buscarPorTag(@Param("tag") String tag);
}
