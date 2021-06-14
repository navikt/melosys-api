package no.nav.melosys.integrasjon.aareg.arbeidsforhold;


import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.felles.SystemContextExchangeFilter;
import no.nav.melosys.integrasjon.medl.MedlemskapRestConsumerConfig;
import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class ArbeidsforholdRestConsumerConfig implements RestConsumer {
    private static final Logger log = LoggerFactory.getLogger(ArbeidsforholdRestConsumerConfig.class);

    private static final String CONSUMER_ID = "srvmelosys";
    private String url;

    @Autowired
    public ArbeidsforholdRestConsumerConfig(@Value("${arbeidsforhold.rest.url}") String url) {
        this.url = url;
    }

    @Bean
    ArbeidsforholdRestConsumer arbeidsforholdRestConsumer(WebClient.Builder webClientBuilder, SystemContextExchangeFilter systemContextExchangeFilter) {
        return new ArbeidsforholdRestConsumerImpl(webClientBuilder
            .baseUrl(url)
            .filter(systemContextExchangeFilter)
            .filter(headerFilter())
            .filter(errorFilter())
            .build());
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
                        return Mono.error(new RuntimeException(errorBody));
                    });
            }
            return Mono.just(response);
        });
    }
}
