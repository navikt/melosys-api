package no.nav.melosys.integrasjon.eessi;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.dto.*;
import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class EessiConsumerImpl implements EessiConsumer {

    private static final Logger log = LoggerFactory.getLogger(EessiConsumerImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String SEDDATA_FILNAVN = "sedData";
    private static final String VEDLEGG_FILNAVN = "vedlegg";

    private static final String SEND_AUTOMATISK = "sendAutomatisk";
    private static final String STATUSER = "statuser";

    EessiConsumerImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto, byte[] vedlegg, BucType bucType, boolean sendAutomatisk) throws MelosysException {

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(String.format("/buc/%s", bucType))
            .queryParam(SEND_AUTOMATISK, sendAutomatisk);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        byte[] sedData;

        try {
            sedData = objectMapper.writeValueAsBytes(sedDataDto);
        } catch (JsonProcessingException jpe) {
            throw new TekniskException("Feil ved parsing av sedDataDto", jpe);
        }

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add(SEDDATA_FILNAVN, lagByteArrayResource(sedData, SEDDATA_FILNAVN));

        if (ArrayUtils.isNotEmpty(vedlegg)) {
            formData.add(VEDLEGG_FILNAVN, lagByteArrayResource(vedlegg, VEDLEGG_FILNAVN));
        }

        return exchange(builder.toUriString(), HttpMethod.POST, new HttpEntity<>(formData, httpHeaders),
            new ParameterizedTypeReference<OpprettSedDto>() {});
    }

    @Override
    public void sendSedPåEksisterendeBuc(SedDataDto sedDataDto, String rinaSaksnummer, SedType sedType) throws MelosysException {
        exchange(String.format("/buc/%s/sed/%s", rinaSaksnummer, sedType), HttpMethod.POST,
            new HttpEntity<>(sedDataDto, getDefaultHeaders()), new ParameterizedTypeReference<Void>() {
            });
    }

    @Override
    public List<Institusjon> hentMottakerinstitusjoner(String bucType, String landkode) throws MelosysException {

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(String.format("/buc/%s/institusjoner", bucType))
            .queryParam("land", landkode);

        List<InstitusjonDto> institusjonDtoList = exchange(builder.toUriString(), HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<InstitusjonDto>>() {
            });

        return institusjonDtoList.stream()
            .map(institusjonDto -> new Institusjon(
                institusjonDto.getId(), institusjonDto.getNavn(), institusjonDto.getLandkode()))
            .collect(Collectors.toList());
    }

    @Override
    public MelosysEessiMelding hentMelosysEessiMeldingFraJournalpostID(String journalpostID) throws MelosysException {
        return exchange(String.format("/journalpost/%s/eessimelding", journalpostID), HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<MelosysEessiMelding>(){
        });
    }

    @Override
    public void lagreSaksrelasjon(SaksrelasjonDto saksrelasjonDto) throws MelosysException {
        exchange("/sak", HttpMethod.POST, new HttpEntity<>(saksrelasjonDto, getDefaultHeaders()),
            new ParameterizedTypeReference<Void>() {});
    }

    @Override
    public List<SaksrelasjonDto> hentSakForRinasaksnummer(String rinaSaksnummer) throws MelosysException {
        return exchange(String.format("/sak?rinaSaksnummer=%s", rinaSaksnummer), HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<SaksrelasjonDto>>() {
        });
    }

    @Override
    public byte[] genererSedForhåndsvisning(SedDataDto sedDataDto, SedType sedType) throws MelosysException {
        return exchange(String.format("/sed/%s/pdf", sedType), HttpMethod.POST,
            new HttpEntity<>(sedDataDto, getDefaultHeaders()), new ParameterizedTypeReference<byte[]>() {
            });
    }

    @Override
    public List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, List<String> statuser) throws MelosysException {
        String uri = UriComponentsBuilder.fromPath(String.format("/sak/%s/bucer/", gsakSaksnummer))
            .queryParam(STATUSER, statuser.toArray()).toUriString();

        List<BucinfoDto> bucinfoDtoList = exchange(uri, HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<BucinfoDto>>() {
        });

        return bucinfoDtoList.stream().map(BucinfoDto::tilDomene).collect(Collectors.toList());
    }

    private <T> T exchange(String uri, HttpMethod method, HttpEntity<?> entity, ParameterizedTypeReference<T> responseType) throws MelosysException {
        try {
            return restTemplate.exchange(uri, method, entity, responseType).getBody();
        } catch (RestClientResponseException e) {
            throw ExceptionMapper.springExTilMelosysEx(e, hentFeilmelding(e));
        }
    }

    private HttpHeaders getDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private ByteArrayResource lagByteArrayResource(byte[] data, String filnavn) {
        return new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return filnavn;
            }
        };
    }

    private String hentFeilmelding(RestClientResponseException ex) {
        try {
            JsonNode jsonNode = objectMapper.readTree(ex.getResponseBodyAsString()).path("message");
            return jsonNode.isMissingNode() ? ex.getMessage() : jsonNode.toString();
        } catch (Exception e) {
            log.warn("Kunne ikke hente ut feilmelding etter kall mot melosys-eessi");
            return ex.getMessage();
        }
    }
}
