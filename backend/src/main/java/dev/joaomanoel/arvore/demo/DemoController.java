package dev.joaomanoel.arvore.demo;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Só existe no perfil demo: fora dele, /api/demo responde 404 e o login não mostra o convite. */
@RestController
@RequestMapping("/api/demo")
@Profile("demo")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    /** Não consulta o banco: serve de health check e de ping pra manter o servidor acordado. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("ativo", true, "duracaoHoras", DemoService.DURACAO.toHours());
    }

    @PostMapping("/sessao")
    public Map<String, String> criarSessao(HttpServletRequest requisicao) {
        return Map.of("token", demoService.criarVisitante(requisicao.getRemoteAddr()));
    }

    @ExceptionHandler(DemoIndisponivelException.class)
    ResponseEntity<Map<String, String>> indisponivel(DemoIndisponivelException erro) {
        return ResponseEntity.status(erro.status()).body(Map.of("erro", erro.getMessage()));
    }
}
