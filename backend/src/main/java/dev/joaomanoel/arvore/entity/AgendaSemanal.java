package dev.joaomanoel.arvore.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agenda_semanal")
@Getter
@Setter
@NoArgsConstructor
public class AgendaSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "atividade_id", nullable = false)
    private UUID atividadeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DiaSemana diaSemana;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void aoPersistir() {
        criadoEm = Instant.now();
    }
}
