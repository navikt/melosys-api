package no.nav.melosys.integrasjon.altinn;

import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

public class SoknadMottakConsumerImpl implements SoknadMottakConsumer {

    private static final Logger log = LoggerFactory.getLogger(SoknadMottakConsumerImpl.class);

    private final WebClient webClient;

    public SoknadMottakConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public MedlemskapArbeidEOSM hentSøknad(final String søknadID) {
        log.info("Henter søknad med ID {}", søknadID);

        return webClient.get()
            .uri("/soknader/{søknadID}", søknadID)
            .accept(MediaType.APPLICATION_XML)
            .retrieve()
            .toEntity(MedlemskapArbeidEOSM.class)
            .block().getBody();
    }

    @Override
    public Collection<AltinnDokument> hentDokumenter(final String søknadID) {
        log.info("Henter dokumenter tilknyttet altinn-søknad {}", søknadID);

        return webClient.get()
            .uri("/soknader/{søknadID}/dokumenter", søknadID)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(new ParameterizedTypeReference<Collection<AltinnDokument>>() {})
            .block().getBody();
    }

    private HttpHeaders getHeaders(MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(mediaType));
        return headers;
    }
}
