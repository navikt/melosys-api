package no.nav.melosys.integrasjon.altinn;

import java.util.Collections;

import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class SoknadMottakConsumerImpl implements SoknadMottakConsumer {

    private static Logger log = LoggerFactory.getLogger(SoknadMottakConsumerImpl.class);

    private final RestTemplate restTemplate;

    public SoknadMottakConsumerImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public MedlemskapArbeidEOSM hentSøknad(String søknadID) {
        log.info("Henter søknad med ID {}", søknadID);

        return restTemplate.exchange(String.format("/soknad/%s", søknadID), HttpMethod.GET,
            new HttpEntity<>(getXmltypeHeaders()), new ParameterizedTypeReference<MedlemskapArbeidEOSM>() {
            }
        ).getBody();
    }

    private HttpHeaders getXmltypeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        return headers;
    }
}
