package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.Atividade;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtividadeRepository extends JpaRepository<Atividade, UUID> {}
