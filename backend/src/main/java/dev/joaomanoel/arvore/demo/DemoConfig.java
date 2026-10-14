package dev.joaomanoel.arvore.demo;

import java.io.IOException;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Perfil {@code demo}: a demonstração pública. Fora dele, nada deste pacote existe.
 *
 * <p>Na imagem da demo (Dockerfile.demo) o backend também serve o frontend, num serviço só: o build
 * do Vite vai pra {@code classpath:/static}. Em produção quem serve é o nginx, como sempre.
 */
@Configuration
@Profile("demo")
@EnableScheduling
class DemoConfig implements WebMvcConfigurer {

    /** Todo DataSource passa a seguir o schema do visitante da requisição. */
    @Bean
    static BeanPostProcessor dataSourceDoVisitante() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String nome) {
                return bean instanceof DataSource original && !(bean instanceof DataSourceDoVisitante)
                        ? new DataSourceDoVisitante(original)
                        : bean;
            }
        };
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // assets têm hash no nome: cache pra sempre
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
        // o resto (index.html, sw.js, manifest) é revalidado sempre, senão versão nova não chega
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new RotaDoSpa());
    }

    /** Arquivo que existe sai como está; rota do React Router (sem extensão) cai no index.html. */
    private static final class RotaDoSpa extends PathResourceResolver {
        @Override
        protected Resource getResource(@NonNull String caminho, @NonNull Resource local) throws IOException {
            Resource pedido = local.createRelative(caminho);
            if (pedido.exists() && pedido.isReadable()) {
                return pedido;
            }
            if (caminho.startsWith("api/") || caminho.substring(caminho.lastIndexOf('/') + 1).contains(".")) {
                return null;
            }
            Resource indice = local.createRelative("index.html");
            return indice.exists() ? indice : null;
        }
    }
}
