package no.nav.melosys.integrasjon.medl;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.felles.SystemContextExchangeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class MedlemskapRestConsumerProducer implements RestConsumer {
    private static final Logger log = LoggerFactory.getLogger(MedlemskapRestConsumerProducer.class);
    private static final String CONSUMER_ID = "srvmelosys";

    private final String url;

    public MedlemskapRestConsumerProducer(@Value("${medlemskap.rest.url}") String url) {
        this.url = url;
    }

    @Bean
    @Primary // Skal denne alltid kalles med system bruker?
    public MedlemskapRestConsumer medlemskapRestConsumer(WebClient.Builder webClientBuilder, SystemContextExchangeFilter systemContextExchangeFilter) {
        return new MedlemskapRestConsumer(
            webClientBuilder
                .baseUrl(url)
                .filter(systemContextExchangeFilter)
                .filter(headerFilter())
                .filter(errorFilter())
                .build()
        );
    }


    private ExchangeFilterFunction headerFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(
            request -> Mono.just(ClientRequest.from(request)
                .header("Nav-Call-Id", getCallID())
                .header("Nav-Consumer-Id", CONSUMER_ID)
                .build())
        );
    }

    private ExchangeFilterFunction errorFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        log.error("Kall mot MEDL feilet. {} - {}", response.statusCode(), errorBody);
                        return Mono.error(new TekniskException("Henting av registeropplysninger fra MEDL feilet."));
                    });
            }
            return Mono.just(response);
        });
    }
}
