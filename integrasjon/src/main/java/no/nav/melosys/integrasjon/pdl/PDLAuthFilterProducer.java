package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PDLAuthFilterProducer {
    @Bean
    @Qualifier("system")
    public PDLAuthFilter pdlSystemAuthFilter(RestStsClient restStsClient) {
        return new PDLAuthFilter(restStsClient, restStsClient::bearerToken);
    }

    @Bean
    @Qualifier("saksbehandler")
    public PDLAuthFilter pdlSaksbehandlerAuthFilter(RestStsClient restStsClient) {
        return new PDLAuthFilter(restStsClient, () -> SubjectHandler.getInstance().getOidcTokenString());
    }
}
