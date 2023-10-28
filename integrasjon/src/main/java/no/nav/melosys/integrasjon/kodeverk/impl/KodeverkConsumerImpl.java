package no.nav.melosys.integrasjon.kodeverk.impl;

import java.time.LocalDate;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import no.nav.melosys.integrasjon.felles.CallIdAware;
import no.nav.melosys.integrasjon.felles.JacksonObjectMapperProvider;
import no.nav.melosys.integrasjon.kodeverk.impl.dto.KodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.config.MDCOperations.X_CORRELATION_ID;
import static no.nav.melosys.config.MDCOperations.getCorrelationId;
import static no.nav.melosys.integrasjon.kodeverk.impl.KodeverkRegisterImpl.BOKMÅL;


public class KodeverkConsumerImpl implements CallIdAware {
    private static final Logger log = LoggerFactory.getLogger(KodeverkConsumerImpl.class);

    private static final String VERSJON = "v1";
    private static final String CONSUMER_ID = "srvmelosys";

    private final WebTarget target;

    KodeverkConsumerImpl(String endpointUrl) {
        log.info("endpointUrl: {}", endpointUrl);
        Client client = ClientBuilder.newBuilder().build();
        target = client.register(JacksonObjectMapperProvider.class).target(endpointUrl);
    }

    public KodeDto hentKodeverk(String navn) {
        log.info("hentKodeverk: {}", navn);
        String path = String.format("/%s/kodeverk/%s/koder/betydninger", VERSJON, navn);
        return target
            .path(path)
            .queryParam("ekskluderUgyldige", false)
            .queryParam("oppslagsdato", LocalDate.MIN)
            .queryParam("spraak", BOKMÅL)
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("Nav-Call-Id", getCallID())
            .header("Nav-Consumer-Id", CONSUMER_ID)
            .header(X_CORRELATION_ID, getCorrelationId())
            .get(KodeDto.class);
    }
}
