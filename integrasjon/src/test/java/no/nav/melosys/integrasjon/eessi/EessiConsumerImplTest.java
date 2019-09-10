package no.nav.melosys.integrasjon.eessi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.dokument.sed.BucType;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.BucInformasjon;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.eessi.melding.SvarAnmodningUnntak;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public class EessiConsumerImplTest {
    private EessiConsumer eessiConsumer;
    private MockRestServiceServer server;
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        eessiConsumer = new EessiConsumerImpl(restTemplate);
        server = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void opprettOgSendSed() throws MelosysException, JsonProcessingException {
        server.expect(requestTo("/sed/createAndSend"))
            .andRespond(withSuccess(
                objectMapper.writeValueAsString(Collections.singletonMap("RinaCaseId", "1234")),
                MediaType.APPLICATION_JSON));

        assertThat(eessiConsumer.opprettOgSendSed(null).get("RinaCaseId")).isEqualToIgnoringCase("1234");
    }

    @Test
    public void hentMottakerinstitusjoner() throws JsonProcessingException, MelosysException {
        InstitusjonDto institusjonDto = new InstitusjonDto();
        institusjonDto.setId("NO:NAVT003");
        institusjonDto.setLandkode("NO");
        institusjonDto.setNavn("NAVT003");

        server.expect(requestTo("/buc/LA_BUC_01/institusjoner"))
            .andRespond(withSuccess(
                objectMapper.writeValueAsString(Collections.singletonList(institusjonDto)),
                MediaType.APPLICATION_JSON));

        List<Institusjon> institusjoner = eessiConsumer.hentMottakerinstitusjoner(BucType.LA_BUC_01.name());
        assertThat(institusjoner.size()).isEqualTo(1);
        assertThat(institusjoner.iterator().next().getId()).isEqualTo("NO:NAVT003");
    }

    @Test
    public void hentMelosysEessiMeldingFraJournalpostID() throws JsonProcessingException, MelosysException {
        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setJournalpostId("123");
        melosysEessiMelding.setBucType("LA_BUC_01");

        server.expect(requestTo("/journalpost/123/eessimelding"))
            .andRespond(withSuccess(objectMapper.writeValueAsString(melosysEessiMelding), MediaType.APPLICATION_JSON));

        MelosysEessiMelding melding = eessiConsumer.hentMelosysEessiMeldingFraJournalpostID("123");
        assertThat(melding.getJournalpostId()).isEqualTo("123");
        assertThat(melding.getBucType()).isEqualTo("LA_BUC_01");
    }

    @Test
    public void lagreSaksrelasjon() throws MelosysException {
        server.expect(requestTo("/sak")).andRespond(withSuccess());
        eessiConsumer.lagreSaksrelasjon(null);
    }

    @Test
    public void hentSakForRinasaksnummer() throws JsonProcessingException, MelosysException {
        List<SaksrelasjonDto> saksrelasjonDto = Collections.singletonList(new SaksrelasjonDto(
            1L,
            "1234",
            BucType.LA_BUC_01.name()
        ));

        server.expect(requestTo("/sak?rinaSaksnummer=1234"))
            .andRespond(withSuccess(objectMapper.writeValueAsString(saksrelasjonDto), MediaType.APPLICATION_JSON));

        List<SaksrelasjonDto> svar = eessiConsumer.hentSakForRinasaksnummer("1234");
        assertThat(svar.size()).isEqualTo(1);
        assertThat(svar.iterator().next().getGsakSaksnummer()).isEqualTo(1L);
        assertThat(svar.iterator().next().getBucType()).isEqualTo(BucType.LA_BUC_01.name());
    }

    @Test
    public void sendAnmodningUnntakSvar() throws MelosysException {
        server.expect(requestTo("/buc/LA_BUC_01/1234/svar")).andRespond(withSuccess());

        SvarAnmodningUnntakDto svarAnmodningUnntakDto = new SvarAnmodningUnntakDto(
            SvarAnmodningUnntak.Beslutning.AVSLAG,
            "begrunnelse",
            new Periode(LocalDate.now(), LocalDate.now().plusMonths(1))
        );
        eessiConsumer.sendAnmodningUnntakSvar(svarAnmodningUnntakDto, "1234");
    }

    @Test
    public void genererSedForhåndsvisning() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        server.expect(requestTo("/sed/A001/pdf"))
            .andRespond(withSuccess(PDF, MediaType.APPLICATION_PDF));

        byte[] pdf = eessiConsumer.genererSedForhåndsvisning(new SedDataDto(), SedType.A001);
        assertThat(pdf).isEqualTo(PDF);
    }

    @Test
    public void opprettBucOgSed() throws JsonProcessingException, MelosysException {
        OpprettSedDto opprettSedDto = new OpprettSedDto();
        opprettSedDto.setRinaUrl("localhost:8080");

        server.expect(requestTo("/sed/create/LA_BUC_01"))
            .andRespond(withSuccess(objectMapper.writeValueAsString(opprettSedDto), MediaType.APPLICATION_JSON));

        String rinaUrl = eessiConsumer.opprettBucOgSed(null, BucType.LA_BUC_01.name());
        assertThat(rinaUrl).isEqualTo("localhost:8080");
    }

    @Test
    public void hentTilknyttedeBucer() throws JsonProcessingException, MelosysException {
        BucinfoDto bucinfoDto = new BucinfoDto();
        bucinfoDto.setId("1234");
        bucinfoDto.setBucType("LA_BUC_01");
        bucinfoDto.setOpprettetDato(Instant.now().toEpochMilli());
        bucinfoDto.setSeder(Collections.emptyList());

        server.expect(requestTo("/sak/123/bucer/?status=utkast"))
            .andRespond(withSuccess(
                objectMapper.writeValueAsString(Collections.singletonList(bucinfoDto)),
                MediaType.APPLICATION_JSON));

        List<BucInformasjon> bucInformasjonListe = eessiConsumer.hentTilknyttedeBucer(123L, "utkast");
        assertThat(bucInformasjonListe.size()).isEqualTo(1);
        assertThat(bucInformasjonListe.iterator().next().getId()).isEqualTo("1234");
    }

    @Test(expected = IntegrasjonException.class)
    public void lagreSaksrelasjon_medError_forventException() throws MelosysException {
        server.expect(requestTo("/sak")).andRespond(withServerError());
        eessiConsumer.lagreSaksrelasjon(null);
    }
}