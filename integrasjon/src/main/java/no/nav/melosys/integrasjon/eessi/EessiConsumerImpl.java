package no.nav.melosys.integrasjon.eessi;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.*;
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
        return exchange("/sed/createAndSend", HttpMethod.POST, new HttpEntity<>(sedDataDto, getDefaultHeaders()), new ParameterizedTypeReference<Map<String, String>>() {
        });
    }

    @Override
    public List<Institusjon> hentMottakerinstitusjoner(String bucType) throws MelosysException {
        List<InstitusjonDto> institusjonDtoList = exchange(String.format("/buc/%s/institusjoner", bucType), HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<InstitusjonDto>>() {
            });

        return institusjonDtoList.stream()
            .map(institusjonDto -> new Institusjon(
                institusjonDto.getId(), institusjonDto.getNavn(), institusjonDto.getLandkode()))
            .collect(Collectors.toList());
    }

    @Override
    public String opprettBucOgSed(SedDataDto sedDataDto, String bucType) throws MelosysException {
        OpprettSedDto opprettSedDto = exchange("/sed/create/" + bucType, HttpMethod.POST,
            new HttpEntity<>(sedDataDto, getDefaultHeaders()), new ParameterizedTypeReference<OpprettSedDto>() {
            });

        return opprettSedDto.getRinaUrl();
    }

    @Override
    public List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, String status) throws MelosysException {
        status = Optional.ofNullable(status).orElse("");

        List<BucinfoDto> bucinfoDtoList = exchange(String.format("/sak/%s/bucer/?status=%s", gsakSaksnummer, status), HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<BucinfoDto>>() {
            });

        return bucinfoDtoList.stream().map(EessiConsumerImpl::tilBucInformasjon).collect(Collectors.toList());
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

    private static BucInformasjon tilBucInformasjon(BucinfoDto bucinfoDto) {
        return new BucInformasjon(
            bucinfoDto.getId(),
            bucinfoDto.getBucType(),
            bucinfoDto.getOpprettetDato(),
            bucinfoDto.getSeder().stream()
                .map(EessiConsumerImpl::tilSedInformasjon)
                .collect(Collectors.toList())
        );
    }

    private static SedInformasjon tilSedInformasjon(SedinfoDto sedinfoDto) {
        return new SedInformasjon(
            sedinfoDto.getBucId(),
            sedinfoDto.getSedId(),
            sedinfoDto.getOpprettetDato(),
            sedinfoDto.getSistOppdatert(),
            sedinfoDto.getSedType(),
            sedinfoDto.getStatus(),
            sedinfoDto.getRinaUrl()
        );
    }
}
