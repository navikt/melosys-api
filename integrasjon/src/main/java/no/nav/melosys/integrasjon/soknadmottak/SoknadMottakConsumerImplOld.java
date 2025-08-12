package no.nav.melosys.integrasjon.soknadmottak;

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

public class SoknadMottakConsumerImplOld implements SoknadMottakConsumer {

    private static final Logger log = LoggerFactory.getLogger(SoknadMottakConsumerImpl.class);

    private final RestTemplate restTemplate;

    public SoknadMottakConsumerImplOld(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public MedlemskapArbeidEOSM hentSøknad(final String søknadID) {
        log.info("Henter søknad med ID {}", søknadID);

        return restTemplate.exchange(
            "/soknader/{søknadID}",
            HttpMethod.GET,
            new HttpEntity<>(getHeaders(MediaType.APPLICATION_XML)),
            MedlemskapArbeidEOSM.class,
            søknadID).getBody();
    }

    @Override
    public Collection<AltinnDokument> hentDokumenter(final String søknadID) {
        log.info("Henter dokumenter tilknyttet altinn-søknad {}", søknadID);

        return restTemplate.exchange(
            "/soknader/{søknadID}/dokumenter",
            HttpMethod.GET,
            new HttpEntity<>(getHeaders(MediaType.APPLICATION_JSON)),
            new ParameterizedTypeReference<Collection<AltinnDokument>>() {},
            søknadID).getBody();
    }

    private HttpHeaders getHeaders(MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(mediaType));
        return headers;
    }
}
