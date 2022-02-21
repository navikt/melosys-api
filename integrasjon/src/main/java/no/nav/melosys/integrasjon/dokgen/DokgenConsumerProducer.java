package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.exception.TekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class DokgenConsumerProducer {
    private static final Logger log = LoggerFactory.getLogger(DokgenConsumerProducer.class);

    private final String url;

    @Autowired
    public DokgenConsumerProducer(@Value("${melosysdokgen.v1.url}") String url) {
        this.url = url;
    }

    @Bean
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
                        return Mono.error(new TekniskException("Kall mot melosys dokument genererings tjeneste feilet"));
                    });
            }
            return Mono.just(response);
        });
    }
}
