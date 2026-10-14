package dev.joaomanoel.arvore.demo;

import org.springframework.http.HttpStatus;

/** Visitante novo recusado: demonstração cheia ou muitas criações do mesmo endereço. */
class DemoIndisponivelException extends RuntimeException {

    private final HttpStatus status;

    DemoIndisponivelException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    HttpStatus status() {
        return status;
    }
}
