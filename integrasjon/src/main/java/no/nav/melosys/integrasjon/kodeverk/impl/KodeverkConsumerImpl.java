package no.nav.melosys.integrasjon.kodeverk.impl;

import java.time.LocalDate;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.kodeverk.impl.dto.KodeDto;

import static no.nav.melosys.integrasjon.kodeverk.impl.KodeverkRegisterImpl.BOKMÅL;

public class KodeverkConsumerImpl implements RestConsumer {

    private final String VERSJON = "v1";
    private final String CONSUMER_ID = "srvmelosys";

    private final WebTarget target;

    KodeverkConsumerImpl(String endpointUrl) {
        Client client = ClientBuilder.newBuilder().build();
        target = client.register(JacksonJsonProvider.class).target(endpointUrl);
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
            .header("Nav-Call-Id", getCallID())
            .header("Nav-Consumer-Id", CONSUMER_ID)
            .get(KodeDto.class);
    }
}
