package no.nav.melosys.integrasjon.eessi;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.eessi.dto.BucinfoDto;
import no.nav.melosys.integrasjon.eessi.dto.InstitusjonDto;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.felles.ExceptionMapper;
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

public class EessiConsumerImpl implements EessiConsumer {

    private static final Logger log = LoggerFactory.getLogger(EessiConsumerImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String SEDDATA_FILNAVN = "sedData";

    private static final String STATUSER = "statuser";

    EessiConsumerImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto,
                                         @Nullable Vedlegg vedlegg,
                                         BucType bucType,
                                         boolean sendAutomatisk) throws MelosysException {

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

        if (vedlegg != null) {
            if (!vedlegg.erGyldig()) {
                throw new TekniskException("Vedlegget er ikke gyldig, kan ikke opprette buc " + bucType);
            }
            ByteArrayResource vedleggRessurs = lagByteArrayResource(vedlegg.getInnhold(), vedlegg.getTittel());
            log.info("Sender vedlegg med størrelse {} for arkivsakID {}",
                vedleggRessurs.contentLength(), sedDataDto.getGsakSaksnummer());
            formData.add("vedlegg", vedleggRessurs);
        }

        return exchange("/buc/{bucType}?sendAutomatisk={sendAutomatisk}", HttpMethod.POST, new HttpEntity<>(formData, httpHeaders),
            new ParameterizedTypeReference<OpprettSedDto>() {}, bucType, sendAutomatisk);
    }

    @Override
    public void sendSedPåEksisterendeBuc(SedDataDto sedDataDto, String rinaSaksnummer, SedType sedType) throws MelosysException {
        exchange("/buc/{bucID}/sed/{sedType}", HttpMethod.POST,
            new HttpEntity<>(sedDataDto, getDefaultHeaders()), new ParameterizedTypeReference<Void>() {
            }, rinaSaksnummer, sedType);
    }

    @Override
    public List<Institusjon> hentMottakerinstitusjoner(String bucType, String landkode) throws MelosysException {

        List<InstitusjonDto> institusjonDtoList = exchange("/buc/{bucType}/institusjoner?land={landKode}", HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<InstitusjonDto>>() {
            }, bucType, landkode);

        return institusjonDtoList.stream()
            .map(institusjonDto -> new Institusjon(
                institusjonDto.getId(), institusjonDto.getNavn(), institusjonDto.getLandkode()))
            .collect(Collectors.toList());
    }

    @Override
    public MelosysEessiMelding hentMelosysEessiMeldingFraJournalpostID(String journalpostID) throws MelosysException {
        return exchange("/journalpost/{journalpostID}/eessimelding", HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<MelosysEessiMelding>(){
            }, journalpostID);
    }

    @Override
    public void lagreSaksrelasjon(SaksrelasjonDto saksrelasjonDto) throws MelosysException {
        exchange("/sak", HttpMethod.POST, new HttpEntity<>(saksrelasjonDto, getDefaultHeaders()),
            new ParameterizedTypeReference<Void>() {});
    }

    @Override
    public List<SaksrelasjonDto> hentSakForRinasaksnummer(String rinaSaksnummer) throws MelosysException {
        return exchange("/sak?rinaSaksnummer={rinaSaksnummer}", HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<SaksrelasjonDto>>() {
            }, rinaSaksnummer);
    }

    @Override
    public byte[] genererSedPdf(SedDataDto sedDataDto, SedType sedType) throws MelosysException {
        return exchange("/sed/{sedType}/pdf", HttpMethod.POST,
            new HttpEntity<>(sedDataDto, getDefaultHeaders()), new ParameterizedTypeReference<byte[]>() {
            }, sedType);
    }

    @Override
    public List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, List<String> statuser) throws MelosysException {

        List<BucinfoDto> bucinfoDtoList = exchange("/sak/{arkivSakID}/bucer?statuser={statuser}", HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<BucinfoDto>>() {
            }, gsakSaksnummer, statuser.toArray());

        return bucinfoDtoList.stream().map(BucinfoDto::tilDomene).collect(Collectors.toList());
    }

    @Override
    public SedGrunnlagDto hentSedGrunnlag(String rinaSaksnummer, String rinaDokumentID) throws MelosysException {
        return exchange("/buc/{rinaSaksnummer}/sed/{rinaDokumentID}/grunnlag", HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<SedGrunnlagDto>() {
            }, rinaSaksnummer, rinaDokumentID);
    }

    private <T> T exchange(String uri, HttpMethod method, HttpEntity<?> entity, ParameterizedTypeReference<T> responseType, Object... variabler) throws MelosysException {
        try {
            return restTemplate.exchange(uri, method, entity, responseType, variabler).getBody();
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
