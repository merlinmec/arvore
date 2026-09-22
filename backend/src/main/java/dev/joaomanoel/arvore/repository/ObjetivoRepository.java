package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Objetivo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjetivoRepository extends JpaRepository<Objetivo, UUID> {

    List<Objetivo> findAllByAreaId(UUID areaId);
}
