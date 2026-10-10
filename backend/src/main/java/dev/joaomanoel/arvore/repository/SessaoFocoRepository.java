package dev.joaomanoel.arvore.repository;

import dev.joaomanoel.arvore.entity.SessaoFoco;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessaoFocoRepository extends JpaRepository<SessaoFoco, UUID> {

    Optional<SessaoFoco> findByFimIsNull();

    /** Sessões que têm algum trecho em [de, ate), inclusive a que está em andamento. */
    @Query("select s from SessaoFoco s where s.inicio < :ate and (s.fim is null or s.fim > :de) order by s.inicio desc")
    List<SessaoFoco> sobrepostas(@Param("de") Instant de, @Param("ate") Instant ate);
}
