package no.nav.melosys.integrasjon.eessi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.*;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.sed.SedDataDto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagA003Dto;
import no.nav.melosys.domain.eessi.sed.SedGrunnlagDto;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public class EessiConsumerTest {


    private final RestTemplate restTemplate = new RestTemplate();

    private EessiConsumer eessiConsumer;
    private MockRestServiceServer server;

    private SedDataDto sedDataDto;

    @Before
    public void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
        eessiConsumer = new EessiConsumerImpl(restTemplate, new ObjectMapper());
        sedDataDto = new SedDataDto();
    }

    @Test
    public void opprettOgSend_forventMap() throws Exception {

        BucType bucType = BucType.LA_BUC_01;
        server.expect(requestTo("/buc/" + bucType + "?sendAutomatisk=true"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andRespond(withSuccess("{\"rinaSaksnummer\":\"12345\",\"rinaUrl\":\"localhost:3000\"}", MediaType.APPLICATION_JSON));

        OpprettSedDto opprettSedDto = eessiConsumer.opprettBucOgSed(sedDataDto, Collections.singleton(new Vedlegg("pdf".getBytes(), "tittel")), bucType, true);
        assertThat(opprettSedDto.getRinaSaksnummer()).isEqualTo("12345");
    }

    @Test(expected = MelosysException.class)
    public void opprettOgSend_forventException() throws Exception {
        BucType bucType = BucType.LA_BUC_01;
        server.expect(requestTo("/buc/" + bucType + "?sendAutomatisk=true"))
            .andRespond(withBadRequest());
        eessiConsumer.opprettBucOgSed(sedDataDto, null, BucType.LA_BUC_01, true);
    }

    @Test
    public void hentTilknyttedeBucer_medEnStatus_forventBucer() throws MelosysException, IOException, URISyntaxException {
        URI uri = (getClass().getClassLoader().getResource("mock/eux/bucer.json")).toURI();
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
    public void hentTilknyttedeBucer_medFlereStatuser_forventRettSti() throws MelosysException {
        server.expect(requestTo("/sak/1/bucer?statuser=UTKAST,MOTTATT,SENDT"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        eessiConsumer.hentTilknyttedeBucer(1L, List.of("UTKAST", "MOTTATT", "SENDT"));
    }

    @Test
    public void hentTilknyttedeBucer_medIngenStatuser_forventRettSti() throws MelosysException {
        server.expect(requestTo("/sak/1/bucer?statuser=SENDT,UTKAST"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        eessiConsumer.hentTilknyttedeBucer(1L, List.of("SENDT", "UTKAST"));
    }

    @Test
    public void hentMottakerinstitusjoner_forventInstitusjoner() throws MelosysException {
        server.expect(requestTo("/buc/LA_BUC_01/institusjoner?land=DE,PL"))
            .andRespond(withSuccess("[{\"id\":\"NO:NAVT002\",\"navn\":\"NAVT002\",\"landkode\":\"NO\"}]",
                MediaType.APPLICATION_JSON));

        List<Institusjon> institusjoner = eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01", List.of("DE", "PL"));
        assertThat(institusjoner).extracting(Institusjon::getId, Institusjon::getNavn, Institusjon::getLandkode)
            .contains(tuple("NO:NAVT002", "NAVT002", "NO"));
    }

    @Test(expected = MelosysException.class)
    public void hentMottakerinstitusjoner_forventException() throws MelosysException {
        server.expect(requestTo("/buc/LA_BUC_01/institusjoner?land=SE"))
            .andRespond(withBadRequest());

        eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01", List.of("SE"));
    }

    @Test
    public void lagreSaksrelasjon_validerUrl() throws MelosysException {
        server.expect(requestTo("/sak"))
            .andRespond(withSuccess());
        eessiConsumer.lagreSaksrelasjon(new SaksrelasjonDto(123L, "123", "123"));
    }

    @Test
    public void hentSakForRinaSaksnummer_validerUrlOgResponse() throws MelosysException {
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
    public void hentMelosysEessiMeldingFraJournalpostID_validerResponse() throws MelosysException, JsonProcessingException {
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
    public void genererSedForhåndsvisning() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        server.expect(requestTo("/sed/A001/pdf"))
            .andRespond(withSuccess(PDF, MediaType.APPLICATION_PDF));

        byte[] pdf = eessiConsumer.genererSedPdf(new SedDataDto(), SedType.A001);
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    public void hentSedGrunnlag_medSedType_rettInstans() throws MelosysException {
        server.expect(requestTo("/buc/1234/sed/abcdef/grunnlag"))
            .andRespond(withSuccess("{\"sedType\": \"A003\"}", MediaType.APPLICATION_JSON));

        String rinaSaksnummer = "1234";
        String rinaDokumentID = "abcdef";
        SedGrunnlagDto response = eessiConsumer.hentSedGrunnlag(rinaSaksnummer, rinaDokumentID);

        assertThat(response).isInstanceOf(SedGrunnlagA003Dto.class);
    }
}