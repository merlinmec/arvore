package dev.joaomanoel.arvore.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conclusao")
@Getter
@Setter
@NoArgsConstructor
public class Conclusao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "atividade_id", nullable = false)
    private UUID atividadeId;

    @Column(nullable = false)
    private LocalDate data;

    /** Quanto foi feito ("5" km), somado na meta quantitativa do objetivo; opcional. */
    @Column(precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void aoPersistir() {
        criadoEm = Instant.now();
    }
}
