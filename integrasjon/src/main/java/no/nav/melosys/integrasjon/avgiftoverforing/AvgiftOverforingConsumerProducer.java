package no.nav.melosys.integrasjon.avgiftoverforing;

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
public class AvgiftOverforingConsumerProducer {

    private static final Logger log = LoggerFactory.getLogger(AvgiftOverforingConsumerProducer.class);

    private final String url;

    @Autowired
    public AvgiftOverforingConsumerProducer(@Value("${melosysavgiftoverforing.url}") String url) {
        this.url = url;
    }

    @Bean
    public AvgiftOverforingConsumer avgiftOverforingConsumer(WebClient.Builder webClientBuilder) {
        return new AvgiftOverforingConsumer(
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
                        log.error("Kall mot melosys-avgift-overføring feilet. {} - {}", response.statusCode(), errorBody);
                        return Mono.error(new TekniskException("Kall mot melosys-avgift-overføring feilet."));
                    });
            }
            return Mono.just(response);
        });
    }
}
