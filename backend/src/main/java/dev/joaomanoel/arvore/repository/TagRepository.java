package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Tag;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByNome(String nome);
}
