package dev.joaomanoel.arvore.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "frequencia_semanal")
@Getter
@Setter
@NoArgsConstructor
public class FrequenciaSemanal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "atividade_id", nullable = false)
    private UUID atividadeId;

    @Column(nullable = false)
    private short vezes;

    /** Primeiro dia em que a meta vale. */
    @Column(name = "vigente_de", nullable = false)
    private LocalDate vigenteDe;

    /** Primeiro dia em que a meta deixa de valer (exclusivo); null enquanto está em vigor. */
    @Column(name = "vigente_ate")
    private LocalDate vigenteAte;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void aoPersistir() {
        criadoEm = Instant.now();
    }
}
