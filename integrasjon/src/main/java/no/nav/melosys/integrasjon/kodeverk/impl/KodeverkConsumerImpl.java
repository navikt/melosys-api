package no.nav.melosys.integrasjon.kodeverk.impl;

import java.time.LocalDate;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import no.nav.melosys.integrasjon.felles.JacksonObjectMapperProvider;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.kodeverk.impl.dto.KodeDto;

import static no.nav.melosys.integrasjon.kodeverk.impl.KodeverkRegisterImpl.BOKMÅL;


public class KodeverkConsumerImpl implements RestConsumer {

    private static final String VERSJON = "v1";
    private static final String CONSUMER_ID = "srvmelosys";

    private final WebTarget target;

    KodeverkConsumerImpl(String endpointUrl) {
        Client client = ClientBuilder.newBuilder().build();
        target = client.register(JacksonObjectMapperProvider.class).target(endpointUrl);
    }

    public KodeDto hentKodeverk(String navn) {
        String path = String.format("/%s/kodeverk/%s/koder/betydninger", VERSJON, navn);
        return target
            .path(path)
            .queryParam("ekskluderUgyldige", false)
            .queryParam("oppslagsdato", LocalDate.MIN)
            .queryParam("spraak", BOKMÅL)
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("Nav-Call-Id", getCorrelationId())
            .header("Nav-Consumer-Id", CONSUMER_ID)
            .get(KodeDto.class);
    }
}
