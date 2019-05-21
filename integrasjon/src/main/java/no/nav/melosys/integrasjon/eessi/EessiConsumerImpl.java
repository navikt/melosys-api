package no.nav.melosys.integrasjon.eessi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import no.nav.melosys.integrasjon.eessi.dto.SedinfoDto;
import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class EessiConsumerImpl implements EessiConsumer {

    private final RestTemplate restTemplate;

    public EessiConsumerImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Map<String, String> opprettOgSendSed(SedDataDto sedDataDto) throws MelosysException {
        return exchange("/sed/createAndSend", HttpMethod.POST, new HttpEntity<>(sedDataDto, getDefaultHeaders()), new ParameterizedTypeReference<Map<String, String>>() {});
    }

    @Override
    public OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto, String bucType) throws MelosysException {
        return exchange("/sed/create/" + bucType, HttpMethod.POST, new HttpEntity<>(sedDataDto, getDefaultHeaders()), new ParameterizedTypeReference<OpprettSedDto>() {});
    }

    @Override
    public List<SedinfoDto> hentTilknyttedeSedUtkast(long gsakSaksnummer) throws MelosysException {
        return exchange("/sed/hentTilknyttedeSedUtkast/" + gsakSaksnummer, HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<SedinfoDto>>() {});
    }

    private <T> T exchange(String uri, HttpMethod method, HttpEntity<?> entity, ParameterizedTypeReference<T> responseType) throws MelosysException {
        try {
            return restTemplate.exchange(uri, method, entity, responseType).getBody();
        } catch (RestClientException e) {
            throw ExceptionMapper.springExTilMelosysEx(e);
        }
    }

    private HttpHeaders getDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
