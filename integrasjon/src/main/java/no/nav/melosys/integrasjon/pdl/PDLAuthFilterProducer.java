package no.nav.melosys.integrasjon.pdl;

import no.finn.unleash.Unleash;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class PDLAuthFilterProducer {
    private static final Logger log = LoggerFactory.getLogger(PDLAuthFilterProducer.class);

    private final Unleash unleash;

    public PDLAuthFilterProducer(Unleash unleash) {
        this.unleash = unleash;
    }

    @Bean
    @Qualifier("system")
    public PDLAuthFilter pdlSystemAuthFilter(RestStsClient restStsClient) {
        if (unleash.isEnabled("melosys.auto.token")) {
            return autoAuthFilter(restStsClient);
        }
        return new PDLAuthFilter(restStsClient, restStsClient::bearerToken);
    }

    @Bean
    @Qualifier("saksbehandler")
    public PDLAuthFilter pdlSaksbehandlerAuthFilter(RestStsClient restStsClient) {
        if (unleash.isEnabled("melosys.auto.token")) {
            return autoAuthFilter(restStsClient);
        }
        return new PDLAuthFilter(restStsClient, () -> "Bearer " + SubjectHandler.getInstance().getOidcTokenString());
    }

    @Bean
    public PDLAuthFilter autoAuthFilter(RestStsClient restStsClient) {
        return new PDLAuthFilter(restStsClient, getStringSupplier(restStsClient));
    }

    private Supplier<String> getStringSupplier(RestStsClient restStsClient) {
        if (ThreadLocalAccessInfo.isProcessCall()) {
            // Funker ikke med å bruke ThreadLocalAccessInfo.isProcessCall siden dette skjer under wirering
            return restStsClient::bearerToken;
        }
        return () -> "Bearer " + SubjectHandler.getInstance().getOidcTokenString();
    }

}
