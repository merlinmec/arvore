package dev.joaomanoel.arvore.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "area")
@Getter
@Setter
@NoArgsConstructor
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "area_pai_id")
    private UUID areaPaiId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "descricao_geral", columnDefinition = "text")
    private String descricaoGeral;

    @Column(name = "cor_tema", length = 20)
    private String corTema;

    @Column(name = "wallpaper_url", length = 500)
    private String wallpaperUrl;

    @Column(nullable = false)
    private int ordem = 0;

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
