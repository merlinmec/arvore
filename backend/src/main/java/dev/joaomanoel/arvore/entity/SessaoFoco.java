package dev.joaomanoel.arvore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Um intervalo de foco numa atividade; {@code fim} nulo é a sessão em andamento. */
@Entity
@Table(name = "sessao_foco")
@Getter
@Setter
@NoArgsConstructor
public class SessaoFoco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "atividade_id", nullable = false, updatable = false)
    private UUID atividadeId;

    @Column(nullable = false, updatable = false)
    private Instant inicio;

    private Instant fim;
}
