package no.nav.melosys.integrasjon.altinn;

import java.util.Collection;
import java.util.Collections;

import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class SoknadMottakConsumerImpl implements SoknadMottakConsumer {

    private static final Logger log = LoggerFactory.getLogger(SoknadMottakConsumerImpl.class);

    private static final String SOKNADER_PATH = "soknader";
    private static final String DOKUMENTER_PATH = "dokumenter";

    private final RestTemplate restTemplate;

    public SoknadMottakConsumerImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public MedlemskapArbeidEOSM hentSøknad(String søknadID) {
        log.info("Henter søknad med ID {}", søknadID);

        String url = UriComponentsBuilder.fromPath(SOKNADER_PATH)
            .pathSegment(søknadID)
            .toUriString();

        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(getHeaders(MediaType.APPLICATION_XML)),
            new ParameterizedTypeReference<MedlemskapArbeidEOSM>() {}).getBody();
    }

    @Override
    public Collection<AltinnDokument> hentDokumenter(String søknadID) {
        log.info("Henter dokumenter tilknyttet altinn-søknad {}", søknadID);
        String url = UriComponentsBuilder.fromPath(SOKNADER_PATH)
            .pathSegment(søknadID, DOKUMENTER_PATH)
            .toUriString();

        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(getHeaders(MediaType.APPLICATION_JSON)),
            new ParameterizedTypeReference<Collection<AltinnDokument>>() {}).getBody();
    }

    private HttpHeaders getHeaders(MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(mediaType));
        return headers;
    }
}
