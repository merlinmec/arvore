package dev.joaomanoel.arvore.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Roda depois da cadeia do Spring Security, com o token já conferido: põe a requisição no schema do
 * visitante (que veio assinado no token) e barra o que não faz sentido na demonstração.
 *
 * <ul>
 *   <li>token sem schema: na demo só existem visitantes, então nada chega ao schema {@code public};
 *   <li>restaurar backup: trocaria os dados de exemplo por um arquivo de fora;
 *   <li>corpo acima de 64 KB: o banco gratuito é pequeno, e nenhuma tela manda tanto.
 * </ul>
 */
@Component
@Profile("demo")
class FiltroDoVisitante extends OncePerRequestFilter {

    private static final long MAX_CORPO = 64 * 1024;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        if ("PUT".equals(request.getMethod()) && request.getRequestURI().equals("/api/dados/backup")) {
            recusar(response, HttpServletResponse.SC_FORBIDDEN, "Restaurar backup fica desligado na demonstração.");
            return;
        }
        if (request.getContentLengthLong() > MAX_CORPO) {
            recusar(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Texto grande demais para a demonstração.");
            return;
        }

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || autenticacao instanceof AnonymousAuthenticationToken) {
            // sem login, a própria cadeia de segurança já respondeu 401 (ou a rota é pública)
            filterChain.doFilter(request, response);
            return;
        }
        if (!(autenticacao.getDetails() instanceof String esquema) || !EsquemaDoVisitante.valido(esquema)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        EsquemaDoVisitante.definir(esquema);
        try {
            filterChain.doFilter(request, response);
        } finally {
            EsquemaDoVisitante.limpar();
        }
    }

    private static void recusar(HttpServletResponse response, int status, String mensagem) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"erro\":\"" + mensagem + "\"}");
    }
}
