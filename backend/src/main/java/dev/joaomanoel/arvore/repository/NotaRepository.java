package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Nota;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaRepository extends JpaRepository<Nota, UUID> {

    List<Nota> findAllByAreaIdOrderByAtualizadoEmDesc(UUID areaId);
}
