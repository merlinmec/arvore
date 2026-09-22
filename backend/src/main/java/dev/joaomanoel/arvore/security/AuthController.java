package dev.joaomanoel.arvore.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String usuarioConfigurado;
    private final String senhaHashConfigurada;

    public AuthController(
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${arvore.auth.usuario}") String usuarioConfigurado,
            @Value("${arvore.auth.senha-hash}") String senhaHashConfigurada) {
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.usuarioConfigurado = usuarioConfigurado;
        this.senhaHashConfigurada = senhaHashConfigurada;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest requisicao) {
        boolean usuarioValido = usuarioConfigurado.equals(requisicao.usuario());
        boolean senhaValida =
                !senhaHashConfigurada.isBlank()
                        && passwordEncoder.matches(requisicao.senha(), senhaHashConfigurada);

        if (!usuarioValido || !senhaValida) {
            return ResponseEntity.status(401).build();
        }

        String token = jwtService.gerarToken(usuarioConfigurado);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    public record LoginRequest(@NotBlank String usuario, @NotBlank String senha) {}

    public record LoginResponse(String token) {}
}
