package no.nav.melosys.integrasjon.eessi;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.arkiv.Vedlegg;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.integrasjon.eessi.dto.*;
import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

public class EessiConsumerWebClient implements EessiConsumer, JsonRestIntegrasjon {

    private static final Logger log = LoggerFactory.getLogger(EessiConsumerWebClient.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    EessiConsumerWebClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto,
                                         Collection<Vedlegg> vedlegg,
                                         BucType bucType,
                                         boolean sendAutomatisk,
                                         boolean oppdaterEksisterendeOmFinnes) {
        return webClient.post()
            .uri("", uriBuilder ->
                uriBuilder
                    .pathSegment("buc", bucType.toString())
                    .queryParam("sendAutomatisk", sendAutomatisk)
                    .queryParam("oppdaterEksisterende", oppdaterEksisterendeOmFinnes)
                    .build())
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(new OpprettBucOgSedDto(sedDataDto, vedlegg))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<OpprettSedDto>() {
            })
            .block();
    }

    @Override
    public void sendSedPåEksisterendeBuc(SedDataDto sedDataDto, String rinaSaksnummer, SedType sedType) {
        webClient.post()
            .uri("", uriBuilder ->
                uriBuilder
                    .pathSegment("buc", rinaSaksnummer)
                    .pathSegment("sed", sedType.toString())
                    .build())
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(sedDataDto)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<OpprettSedDto>() {
            })
            .block();
    }

    @Override
    public List<Institusjon> hentMottakerinstitusjoner(String bucType, Collection<String> landkoder) {
        List<InstitusjonDto> institusjonDtoList = webClient.get()
            .uri("", uriBuilder ->
                uriBuilder
                    .pathSegment("buc", bucType)
                    .pathSegment("institusjoner")
                    .queryParam("land", String.join(",", landkoder))
                    .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<InstitusjonDto>>() {
            })
            .block();

        return institusjonDtoList.stream()
            .map(institusjonDto -> new Institusjon(
                institusjonDto.getId(), institusjonDto.getNavn(), institusjonDto.getLandkode()))
            .toList();
    }

    @Override
    public MelosysEessiMelding hentMelosysEessiMeldingFraJournalpostID(String journalpostID) {
        return webClient.get()
            .uri("", uriBuilder ->
                uriBuilder
                    .pathSegment("journalpost", journalpostID, "eessimelding")
                    .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<MelosysEessiMelding>() {
            })
            .block();
    }

    @Override
    public void lagreSaksrelasjon(SaksrelasjonDto saksrelasjonDto) {
        webClient.post()
            .uri("/sak")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(saksrelasjonDto)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    @Override
    public List<SaksrelasjonDto> hentSakForRinasaksnummer(String rinaSaksnummer) {
        return webClient.get()
            .uri("", uriBuilder ->
                uriBuilder
                    .pathSegment("sak")
                    .queryParam("rinaSaksnummer", rinaSaksnummer)
                    .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<SaksrelasjonDto>>() {
            })
            .block();
    }

    @Override
    public byte[] genererSedPdf(SedDataDto sedDataDto, SedType sedType) {
        return exchange("/sed/{sedType}/pdf", HttpMethod.POST,
            new HttpEntity<>(sedDataDto, getDefaultHeaders()), new ParameterizedTypeReference<byte[]>() {
            }, sedType);
    }

    @Override
    public List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, List<String> statuser) {

        List<BucinfoDto> bucinfoDtoList = exchange("/sak/{arkivSakID}/bucer?statuser={statuser}", HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<List<BucinfoDto>>() {
            }, gsakSaksnummer, statuser.toArray());

        return bucinfoDtoList.stream().map(BucinfoDto::tilDomene).collect(Collectors.toList());
    }

    @Override
    public SedGrunnlagDto hentSedGrunnlag(String rinaSaksnummer, String rinaDokumentID) {
        return exchange("/buc/{rinaSaksnummer}/sed/{rinaDokumentID}/grunnlag", HttpMethod.GET,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<SedGrunnlagDto>() {
            }, rinaSaksnummer, rinaDokumentID);
    }

    @Override
    public void lukkBuc(String rinaSaksnummer) {
        exchange("/buc/{rinaSaksnummer}/lukk", HttpMethod.POST,
            new HttpEntity<>(getDefaultHeaders()), new ParameterizedTypeReference<Void>() {
            }, rinaSaksnummer);
    }

    @Override
    public List<String> hentMuligeAksjoner(String rinaSaksnummer) {
        return webClient.get()
            .uri("", uriBuilder ->
                uriBuilder
                    .pathSegment("buc", rinaSaksnummer, "aksjoner")
                    .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<String>>() {
            })
            .block();
    }


    private <T> T exchange(String uri, HttpMethod method, HttpEntity<?> entity, ParameterizedTypeReference<T> responseType, Object... variabler) {
        try {
            return null;
        } catch (RestClientResponseException e) {
            throw ExceptionMapper.mapException(e, hentFeilmelding(e));
        }
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
