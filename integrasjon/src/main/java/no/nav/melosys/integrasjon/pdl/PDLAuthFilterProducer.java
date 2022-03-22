package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PDLAuthFilterProducer {
    private static final Logger log = LoggerFactory.getLogger(PDLAuthFilterProducer.class);

    @Bean
    @Qualifier("system")
    public PDLAuthFilter pdlSystemAuthFilter(RestStsClient restStsClient) {
        if (ThreadLocalAccessInfo.isFrontendCall()) { // Debug only
            ThreadLocalAccessInfo.warnFrontendCall(this, "PDLAuthFilterProducer");
            log.warn("Blir kalt fra forntend\n{}", ThreadLocalAccessInfo.getInfo());
        }

        return new PDLAuthFilter(restStsClient, restStsClient::bearerToken);
    }

    @Bean
    @Qualifier("saksbehandler")
    public PDLAuthFilter pdlSaksbehandlerAuthFilter(RestStsClient restStsClient) {
        if (ThreadLocalAccessInfo.isProcessCall()) { // Debug only
            ThreadLocalAccessInfo.warnFrontendCall(this, "PDLAuthFilterProducer");
            log.warn("Blir kalt fra prosess\n{}", ThreadLocalAccessInfo.getInfo());
        }
        return new PDLAuthFilter(restStsClient, () -> "Bearer " + SubjectHandler.getInstance().getOidcTokenString());
    }
}
