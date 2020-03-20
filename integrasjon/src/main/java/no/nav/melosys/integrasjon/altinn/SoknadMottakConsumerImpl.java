package no.nav.melosys.integrasjon.altinn;

import java.util.Collections;

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
    public String hentSøknad(String søknadID) {
        String xml = restTemplate.exchange(String.format("/soknad/%s", søknadID), HttpMethod.GET,
            new HttpEntity<>(getXmltypeHeaders()), new ParameterizedTypeReference<String>() {
            }
        ).getBody();
        log.debug("{}", xml);

        // todo Legge inn avhengighet til XSD (når den er klar) - MELOSYS-3572
        return xml;
    }

    private HttpHeaders getXmltypeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
        return headers;
    }
}
