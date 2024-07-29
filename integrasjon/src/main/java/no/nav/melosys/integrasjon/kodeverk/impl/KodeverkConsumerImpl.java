package no.nav.melosys.integrasjon.kodeverk.impl;

import java.time.LocalDate;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import no.nav.melosys.integrasjon.felles.CallIdAware;
import no.nav.melosys.integrasjon.felles.JacksonObjectMapperProvider;
import no.nav.melosys.integrasjon.kodeverk.impl.dto.FellesKodeverkDto;

import static no.nav.melosys.config.MDCOperations.X_CORRELATION_ID;
import static no.nav.melosys.config.MDCOperations.getCorrelationId;
import static no.nav.melosys.integrasjon.kodeverk.impl.KodeverkRegisterImpl.BOKMÅL;


public class KodeverkConsumerImpl implements CallIdAware {

    private static final String VERSJON = "v1";
    private static final String CONSUMER_ID = "srvmelosys";

    private final WebTarget target;

    KodeverkConsumerImpl(String endpointUrl) {
        Client client = ClientBuilder.newBuilder().build();
        target = client.register(JacksonObjectMapperProvider.class).target(endpointUrl);
    }

    public FellesKodeverkDto hentKodeverk(String navn) {
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
            .get(FellesKodeverkDto.class);
    }
}
