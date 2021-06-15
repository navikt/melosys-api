package no.nav.melosys.integrasjon.dokgen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class DokgenConsumerConfig {
    private static final Logger log = LoggerFactory.getLogger(DokgenConsumerConfig.class);

    private final String url;

    @Autowired
    public DokgenConsumerConfig(@Value("${melosysdokgen.v1.url}") String url) {
        this.url = url;
    }

    @Bean
    @Primary
    public DokgenConsumer dokgenConsumer(WebClient.Builder webClientBuilder) {
        return new DokgenConsumer(
            webClientBuilder
                .baseUrl(url)
                .filter(errorFilter())
                .build()
        );
    }

    private ExchangeFilterFunction errorFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        log.error("Kall mot melosys-dokgen feilet. {} - {}", response.statusCode(), errorBody);
                        return Mono.error(new RuntimeException(errorBody));
                    });
            }
            return Mono.just(response);
        });
    }
}
