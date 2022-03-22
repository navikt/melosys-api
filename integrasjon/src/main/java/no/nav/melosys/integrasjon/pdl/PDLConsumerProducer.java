package no.nav.melosys.integrasjon.pdl;

import java.util.Collections;

import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PDLConsumerProducer {
    private static final Logger log = LoggerFactory.getLogger(PDLConsumerProducer.class);

    private static final String TEMA_HEADER = "Tema";
    private static final String TEMA_HEADER_MEDLEMSKAP_VERDI = "MED";

    @Bean
    @Qualifier("system")
    public PDLConsumer pdlConsumer(WebClient.Builder webclientBuilder,
                                   @Value("${PDL.url}") String pdlUrl,
                                   @Qualifier("system") PDLAuthFilter pdlSystemAuthFilter) {
        if (ThreadLocalAccessInfo.isFrontendCall()) { // Debug only
            ThreadLocalAccessInfo.warnFrontendCall(this, "PDLAuthFilterProducer");
            log.warn("Blir kalt fra forntend\n{}", ThreadLocalAccessInfo.getInfo());
        }
        return new PDLConsumerImpl(
            webclientBuilder(webclientBuilder, pdlUrl)
                .filter(pdlSystemAuthFilter)
                .build()
        );
    }

    @Bean
    @Qualifier("saksbehandler")
    public PDLConsumer pdlConsumerForSaksbehandler(WebClient.Builder webclientBuilder,
                                                   @Value("${PDL.url}") String pdlUrl,
                                                   @Qualifier("saksbehandler") PDLAuthFilter pdlSaksbehandlerAuthFilter) {
        if (ThreadLocalAccessInfo.isProcessCall()) { // Debug only
            ThreadLocalAccessInfo.warnFrontendCall(this, "PDLAuthFilterProducer");
            log.warn("Blir kalt fra prosess\n{}", ThreadLocalAccessInfo.getInfo());
        }
        return new PDLConsumerImpl(
            webclientBuilder(webclientBuilder, pdlUrl).filter(pdlSaksbehandlerAuthFilter).build());
    }

    private WebClient.Builder webclientBuilder(WebClient.Builder webclientBuilder, String pdlUrl) {
        return webclientBuilder
            .baseUrl(pdlUrl)
            .defaultHeaders(this::defaultHeaders);
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(TEMA_HEADER, TEMA_HEADER_MEDLEMSKAP_VERDI);
    }
}
