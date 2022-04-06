package no.nav.melosys.integrasjon.pdl;

import no.finn.unleash.Unleash;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PDLAuthFilterProducer {
    private final Unleash unleash;

    public PDLAuthFilterProducer(Unleash unleash) {
        this.unleash = unleash;
    }

    @Bean
    @Qualifier("system")
    public PDLAuthFilter pdlSystemAuthFilter(RestStsClient restStsClient) {
        return new PDLAuthFilter(restStsClient, restStsClient::bearerToken, unleash);
    }

    @Bean
    @Qualifier("saksbehandler")
    public PDLAuthFilter pdlSaksbehandlerAuthFilter(RestStsClient restStsClient) {
        return new PDLAuthFilter(restStsClient, () -> "Bearer " + SubjectHandler.getInstance().getOidcTokenString(), unleash);
    }
}
