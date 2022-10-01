package no.nav.melosys.integrasjon.eessi;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import no.nav.melosys.domain.arkiv.Vedlegg;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.integrasjon.eessi.dto.BucinfoDto;
import no.nav.melosys.integrasjon.eessi.dto.InstitusjonDto;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

public class EessiConsumerWebClient implements EessiConsumer, JsonRestIntegrasjon {

    private final WebClient webClient;

    EessiConsumerWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public OpprettSedDto opprettBucOgSed(SedDataDto sedDataDto,
                                         Collection<Vedlegg> vedlegg,
                                         BucType bucType,
                                         boolean sendAutomatisk,
                                         boolean oppdaterEksisterendeOmFinnes) {
        return httpPost(uriBuilder ->
                uriBuilder
                    .pathSegment("buc", bucType.toString())
                    .queryParam("sendAutomatisk", sendAutomatisk)
                    .queryParam("oppdaterEksisterende", oppdaterEksisterendeOmFinnes)
                    .build(),
            sedDataDto, new ParameterizedTypeReference<>() {
            });
    }

    @Override
    public void sendSedPåEksisterendeBuc(SedDataDto sedDataDto, String rinaSaksnummer, SedType sedType) {
        httpPost(uriBuilder ->
                uriBuilder
                    .pathSegment("buc", rinaSaksnummer)
                    .pathSegment("sed", sedType.toString()).build(),
            sedDataDto, new ParameterizedTypeReference<OpprettSedDto>() {
            });
    }

    @Override
    public List<Institusjon> hentMottakerinstitusjoner(String bucType, Collection<String> landkoder) {
        List<InstitusjonDto> institusjonDtoList = httpGet(uriBuilder ->
            uriBuilder
                .pathSegment("buc", bucType)
                .pathSegment("institusjoner")
                .queryParam("land", String.join(",", landkoder))
                .build(), new ParameterizedTypeReference<>() {
        });

        return institusjonDtoList.stream()
            .map(institusjonDto -> new Institusjon(
                institusjonDto.getId(), institusjonDto.getNavn(), institusjonDto.getLandkode()))
            .toList();
    }

    @Override
    public MelosysEessiMelding hentMelosysEessiMeldingFraJournalpostID(String journalpostID) {
        return httpGet(uriBuilder ->
            uriBuilder
                .pathSegment("journalpost", journalpostID, "eessimelding")
                .build(), new ParameterizedTypeReference<>() {
        });
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
        return httpGet(uriBuilder ->
            uriBuilder
                .pathSegment("sak")
                .queryParam("rinaSaksnummer", rinaSaksnummer)
                .build(), new ParameterizedTypeReference<>() {
        });
    }

    @Override
    public byte[] genererSedPdf(SedDataDto sedDataDto, SedType sedType) {
        return httpPost(uriBuilder ->
                uriBuilder
                    .pathSegment("sed", sedType.toString(), "pdf")
                    .build(),
            sedDataDto, new ParameterizedTypeReference<>() {
            });
    }

    @Override
    public List<BucInformasjon> hentTilknyttedeBucer(long gsakSaksnummer, List<String> statuser) {
        List<BucinfoDto> bucinfoDtoList = httpGet(uriBuilder ->
            uriBuilder
                .pathSegment("sak", String.valueOf(gsakSaksnummer), "bucer")
                .queryParam("statuser", String.join(",", statuser))
                .build(), new ParameterizedTypeReference<>() {
        });

        return bucinfoDtoList.stream().map(BucinfoDto::tilDomene).toList();
    }

    @Override
    public SedGrunnlagDto hentSedGrunnlag(String rinaSaksnummer, String rinaDokumentID) {
        return httpGet(uriBuilder ->
            uriBuilder
                .pathSegment("buc", rinaSaksnummer)
                .pathSegment("sed", rinaDokumentID, "grunnlag")
                .build(), new ParameterizedTypeReference<>() {
        });
    }

    @Override
    public void lukkBuc(String rinaSaksnummer) {
        httpPost(uriBuilder ->
            uriBuilder
                .pathSegment("buc", rinaSaksnummer, "lukk")
                .build(),  new ParameterizedTypeReference<Void>() {
        });
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

    private <T> T httpGet(Function<UriBuilder, URI> uriFunction, ParameterizedTypeReference<T> responseType) {
        return webClient.get()
            .uri("", uriFunction)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(responseType)
            .block();
    }

    private <T> T httpPost(
        Function<UriBuilder, URI> uriFunction,
        Object bodyValue,
        ParameterizedTypeReference<T> responseType
    ) {
        return webClient.post()
            .uri("", uriFunction)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(bodyValue)
            .retrieve()
            .bodyToMono(responseType)
            .block();
    }

    private <T> T httpPost(
        Function<UriBuilder, URI> uriFunction,
        ParameterizedTypeReference<T> responseType
    ) {
        return webClient.post()
            .uri("", uriFunction)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(responseType)
            .block();
    }
}
