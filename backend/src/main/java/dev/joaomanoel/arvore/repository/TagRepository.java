package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByNome(String nome);

    /** Nomes das tags usadas por alguma nota, em ordem alfabética. */
    @Query(
            value = "select distinct t.nome from tag t join nota_tag nt on nt.tag_id = t.id order by t.nome",
            nativeQuery = true)
    List<String> nomesEmUso();

    /** Apaga as tags que ficaram sem nenhuma nota (depois de editar ou excluir notas). */
    @Modifying
    @Query(
            value = "delete from tag t where not exists (select 1 from nota_tag nt where nt.tag_id = t.id)",
            nativeQuery = true)
    int apagarOrfas();
}
