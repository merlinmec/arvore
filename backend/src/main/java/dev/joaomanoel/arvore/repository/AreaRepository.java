package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Area;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaRepository extends JpaRepository<Area, UUID> {

    List<Area> findAllByOrderByOrdemAsc();

    long countByAreaPaiId(UUID areaPaiId);
}
