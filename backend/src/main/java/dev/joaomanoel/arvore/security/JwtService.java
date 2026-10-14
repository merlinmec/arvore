package dev.joaomanoel.arvore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String USUARIO_VISITANTE = "visitante";
    private static final String CLAIM_ESQUEMA = "esquema";

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(
            @Value("${arvore.jwt.segredo}") String segredo,
            @Value("${arvore.jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(String usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    /**
     * Token de um visitante da demonstração: o schema dele vai numa claim assinada, então ninguém
     * troca de schema editando o token. Vence junto com o schema, que é apagado depois disso.
     */
    public String gerarTokenDeVisitante(String esquema, Instant venceEm) {
        return Jwts.builder()
                .subject(USUARIO_VISITANTE)
                .claim(CLAIM_ESQUEMA, esquema)
                .issuedAt(new Date())
                .expiration(Date.from(venceEm))
                .signWith(chave)
                .compact();
    }

    /** Lança exceção se a assinatura não confere ou o token venceu. */
    public Sessao ler(String token) {
        Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
        return new Sessao(claims.getSubject(), claims.get(CLAIM_ESQUEMA, String.class));
    }

    /** {@code esquema} só existe no token de visitante da demonstração. */
    public record Sessao(String usuario, String esquema) {}
}
