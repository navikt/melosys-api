package no.nav.melosys.integrasjon.eessi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagA003Dto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class EessiConsumerTest {
    private final RestTemplate restTemplate = new RestTemplate();

    private EessiConsumer eessiConsumer;
    private MockRestServiceServer server;

    private SedDataDto sedDataDto;

    @BeforeEach
    void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
        eessiConsumer = new EessiConsumerImpl(restTemplate, new ObjectMapper());
        sedDataDto = new SedDataDto();
    }

    @Test
    void opprettOgSend_forventMap() throws Exception {

        BucType bucType = BucType.LA_BUC_01;
        server.expect(requestTo("/buc/" + bucType + "?sendAutomatisk=true&oppdaterEksisterende=true"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andRespond(withSuccess("{\"rinaSaksnummer\":\"12345\",\"rinaUrl\":\"localhost:3000\"}", MediaType.APPLICATION_JSON));

        OpprettSedDto opprettSedDto = eessiConsumer.opprettBucOgSed(sedDataDto, Collections.singleton(new Vedlegg("pdf".getBytes(), "tittel")), bucType, true, true);
        assertThat(opprettSedDto.getRinaSaksnummer()).isEqualTo("12345");
    }

    @Test
    void opprettOgSend_forventException() {
        BucType bucType = BucType.LA_BUC_01;
        server.expect(requestTo("/buc/" + bucType + "?sendAutomatisk=true&oppdaterEksisterende=true"))
            .andRespond(withBadRequest());
        assertThatExceptionOfType(IntegrasjonException.class).isThrownBy(
            () -> eessiConsumer.opprettBucOgSed(sedDataDto, null, BucType.LA_BUC_01, true, true)
        );
    }

    @Test
    void hentTilknyttedeBucer_medEnStatus_forventBucer() throws IOException, URISyntaxException {
        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("mock/eux/bucer.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(uri)));

        server.expect(requestTo("/sak/1/bucer?statuser=UTKAST"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<BucInformasjon> bucInformasjonListe = eessiConsumer.hentTilknyttedeBucer(1L, Collections.singletonList("UTKAST"));
        assertThat(bucInformasjonListe)
            .extracting(BucInformasjon::getId, BucInformasjon::getBucType)
            .contains(
                tuple("111111", "LA_BUC_03"),
                tuple("222222", "LA_BUC_01")
            );

        assertThat(bucInformasjonListe.get(0).getSeder())
            .extracting(SedInformasjon::getSedId, SedInformasjon::getSedType, SedInformasjon::getStatus)
            .contains(tuple("22223333", "A008", "UTKAST"));

        assertThat(bucInformasjonListe.get(1).getSeder())
            .extracting(SedInformasjon::getSedId, SedInformasjon::getSedType, SedInformasjon::getStatus)
            .contains(
                tuple("11221122", "A002", "UTKAST"),
                tuple("11332233", "A001", "UTKAST")
            );
    }

    @Test
    void hentTilknyttedeBucer_medFlereStatuser_forventRettSti() {
        server.expect(requestTo("/sak/1/bucer?statuser=UTKAST,MOTTATT,SENDT"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        eessiConsumer.hentTilknyttedeBucer(1L, List.of("UTKAST", "MOTTATT", "SENDT"));
    }

    @Test
    void hentTilknyttedeBucer_medIngenStatuser_forventRettSti() {
        server.expect(requestTo("/sak/1/bucer?statuser=SENDT,UTKAST"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        eessiConsumer.hentTilknyttedeBucer(1L, List.of("SENDT", "UTKAST"));
    }

    @Test
    void hentMottakerinstitusjoner_forventInstitusjoner() {
        server.expect(requestTo("/buc/LA_BUC_01/institusjoner?land=DE,PL"))
            .andRespond(withSuccess("[{\"id\":\"NO:NAVT002\",\"navn\":\"NAVT002\",\"landkode\":\"NO\"}]",
                MediaType.APPLICATION_JSON));

        List<Institusjon> institusjoner = eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01", List.of("DE", "PL"));
        assertThat(institusjoner).extracting(Institusjon::id, Institusjon::navn, Institusjon::landkode)
            .contains(tuple("NO:NAVT002", "NAVT002", "NO"));
    }

    @Test
    void hentMottakerinstitusjoner_forventException() {
        final List<String> landkoder = List.of("SE");
        server.expect(requestTo("/buc/LA_BUC_01/institusjoner?land=SE"))
            .andRespond(withBadRequest());
        assertThatExceptionOfType(IntegrasjonException.class).isThrownBy(
            () -> eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01", landkoder)
        );
    }

    @Test
    void lagreSaksrelasjon_validerUrl() {
        server.expect(requestTo("/sak"))
            .andRespond(withSuccess());
        eessiConsumer.lagreSaksrelasjon(new SaksrelasjonDto(123L, "123", "123"));
    }

    @Test
    void hentSakForRinaSaksnummer_validerUrlOgResponse() {
        final String rinaSaksnummer = "114422";
        final long gsakSaksnummer = 123L;
        final String bucType = "LA_BUC_04";

        server.expect(requestTo("/sak?rinaSaksnummer=" + rinaSaksnummer))
            .andRespond(withSuccess("[{\"rinaSaksnummer\":\"" + rinaSaksnummer + "\"," +
                    " \"gsakSaksnummer\":" + gsakSaksnummer + "," +
                    " \"bucType\":\"" + bucType + "\"}]",
                MediaType.APPLICATION_JSON));

        List<SaksrelasjonDto> response = eessiConsumer.hentSakForRinasaksnummer(rinaSaksnummer);
        assertThat(response).hasSize(1);

        SaksrelasjonDto saksrelasjonDto = response.get(0);
        assertThat(saksrelasjonDto.getRinaSaksnummer()).isEqualTo(rinaSaksnummer);
        assertThat(saksrelasjonDto.getGsakSaksnummer()).isEqualTo(gsakSaksnummer);
        assertThat(saksrelasjonDto.getBucType()).isEqualTo(bucType);
    }

    @Test
    void hentMelosysEessiMeldingFraJournalpostID_validerResponse() throws JsonProcessingException {
        final String journalpostID = "115314";
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setSedType("A009");
        melosysEessiMelding.setJournalpostId(journalpostID);

        server.expect(requestTo("/journalpost/" + journalpostID + "/eessimelding"))
            .andRespond(withSuccess(new ObjectMapper().writeValueAsString(melosysEessiMelding), MediaType.APPLICATION_JSON));

        MelosysEessiMelding response = eessiConsumer.hentMelosysEessiMeldingFraJournalpostID(journalpostID);

        assertThat(response.getSedType()).isEqualTo(melosysEessiMelding.getSedType());
        assertThat(response.getJournalpostId()).isEqualTo(melosysEessiMelding.getJournalpostId());
    }

    @Test
    void genererSedForhåndsvisning() {
        final byte[] PDF = "pdf".getBytes();
        server.expect(requestTo("/sed/A001/pdf"))
            .andRespond(withSuccess(PDF, MediaType.APPLICATION_PDF));

        byte[] pdf = eessiConsumer.genererSedPdf(new SedDataDto(), SedType.A001);
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    void hentSedGrunnlag_medSedType_rettInstans() {
        server.expect(requestTo("/buc/1234/sed/abcdef/grunnlag"))
            .andRespond(withSuccess("{\"sedType\": \"A003\"}", MediaType.APPLICATION_JSON));

        String rinaSaksnummer = "1234";
        String rinaDokumentID = "abcdef";
        SedGrunnlagDto response = eessiConsumer.hentSedGrunnlag(rinaSaksnummer, rinaDokumentID);

        assertThat(response).isInstanceOf(SedGrunnlagA003Dto.class);
    }
}
