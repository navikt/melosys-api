package no.nav.melosys.integrasjon.eessi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SaksrelasjonDto;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public class EessiConsumerTest {

    @Spy
    private RestTemplate restTemplate;
    private EessiConsumer eessiConsumer;
    private MockRestServiceServer server;

    private SedDataDto sedDataDto;

    @Before
    public void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
        eessiConsumer = new EessiConsumerImpl(restTemplate);
        sedDataDto = new SedDataDto();
    }

    @Test
    public void opprettOgSend_forventMap() throws Exception {
        BucType bucType = BucType.LA_BUC_01;
        server.expect(requestTo("/buc/" + bucType + "?forsokSend=true"))
            .andRespond(withSuccess("{\"rinaSaksnummer\":\"12345\",\"rinaUrl\":\"localhost:3000\"}", MediaType.APPLICATION_JSON));
        OpprettSedDto opprettSedDto = eessiConsumer.opprettBucOgSed(sedDataDto, bucType, true);
        assertThat(opprettSedDto.getRinaSaksnummer()).isEqualTo("12345");
    }

    @Test(expected = MelosysException.class)
    public void opprettOgSend_forventException() throws Exception {
        BucType bucType = BucType.LA_BUC_01;
        server.expect(requestTo("/buc/" + bucType + "?forsokSend=true"))
            .andRespond(withBadRequest());
        eessiConsumer.opprettBucOgSed(sedDataDto, BucType.LA_BUC_01, true);
    }

    @Test
    public void hentTilknyttedeBucer_forventBucer() throws MelosysException, IOException, URISyntaxException {
        URI uri = (getClass().getClassLoader().getResource("mock/eux/bucer.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(uri)));

        server.expect(requestTo("/sak/1/bucer/?status=utkast"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<BucInformasjon> bucInformasjonListe = eessiConsumer.hentTilknyttedeBucer(1L, "utkast");
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

    @Test(expected = MelosysException.class)
    public void hentTilknyttedeBucer_forventException() throws MelosysException {
        server.expect(requestTo("/sak/1/bucer/?status=utkast"))
            .andRespond(withBadRequest());

        eessiConsumer.hentTilknyttedeBucer(1L, "utkast");
    }

    @Test
    public void hentMottakerinstitusjoner_forventInstitusjoner() throws MelosysException {
        server.expect(requestTo("/buc/LA_BUC_01/institusjoner"))
            .andRespond(withSuccess("[{\"id\":\"NO:NAVT002\",\"navn\":\"NAVT002\",\"landkode\":\"NO\"}]",
                MediaType.APPLICATION_JSON));

        List<Institusjon> institusjoner = eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01");
        assertThat(institusjoner).extracting(Institusjon::getId, Institusjon::getNavn, Institusjon::getLandkode)
            .contains(tuple("NO:NAVT002", "NAVT002", "NO"));
    }

    @Test(expected = MelosysException.class)
    public void hentMottakerinstitusjoner_forventException() throws MelosysException {
        server.expect(requestTo("/buc/LA_BUC_01/institusjoner"))
            .andRespond(withBadRequest());

        eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01");
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
}