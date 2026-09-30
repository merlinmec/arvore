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
@Table(name = "objetivo")
@Getter
@Setter
@NoArgsConstructor
public class Objetivo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(nullable = false, columnDefinition = "text")
    private String descricao;

    private LocalDate prazo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusObjetivo status = StatusObjetivo.ATIVO;

    /** Meta quantitativa opcional ("100" + "km"); os dois campos vêm juntos ou nenhum. */
    @Column(name = "meta_valor", precision = 12, scale = 2)
    private BigDecimal metaValor;

    @Column(name = "meta_unidade", length = 20)
    private String metaUnidade;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void aoPersistir() {
        Instant agora = Instant.now();
        criadoEm = agora;
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }
}
