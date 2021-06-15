package no.nav.melosys.integrasjon.avgiftoverforing;

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
public class AvgiftOverforingConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(AvgiftOverforingConsumerConfig.class);

    private final String url;

    @Autowired
    public AvgiftOverforingConsumerConfig(@Value("${melosysavgiftoverforing.url}") String url) {
        this.url = url;
    }

    @Bean
    @Primary
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
                        log.error("Kall mot melosys-avgift-overforing feilet. {} - {}", response.statusCode(), errorBody);
                        return Mono.error(new RuntimeException(errorBody));
                    });
            }
            return Mono.just(response);
        });
    }
}
