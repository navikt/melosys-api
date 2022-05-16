package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.integrasjon.reststs.RestSts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PDLAuthFilterProducer {
    @Bean
    @Qualifier("system")
    public PDLAuthFilter pdlSystemAuthFilter(RestSts restSts) {
        return new PDLAuthFilter(restSts);
    }

    @Bean
    @Qualifier("saksbehandler")
    public PDLAuthFilter pdlSaksbehandlerAuthFilter(RestSts restSts) {
        return new PDLAuthFilter(restSts);
    }
}
