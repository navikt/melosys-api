package no.nav.melosys.integrasjon.eux.consumer;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import no.nav.melosys.eux.model.medlemskap.impl.MedlemskapA009;
import no.nav.melosys.eux.model.nav.SED;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public class EuxConsumerTest {

    @Spy
    private RestTemplate restTemplate;

    @Mock
    private RestStsClient restStsClient;

    @InjectMocks
    private EuxConsumerImpl euxConsumer;

    private MockRestServiceServer server;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void hentBuC_returnerObjekt() throws MelosysException {
        String id = "1234";
        server.expect(requestTo("/BuC?RINASaksnummer=" + id))
            .andRespond(withSuccess("1234", MediaType.APPLICATION_JSON));

        JsonNode response = euxConsumer.hentBuC(id);
        assertNotNull(response);
    }

    @Test
    public void opprettBuC_returnererId() throws MelosysException {
        String id = "1234";
        String buc = "LA_BUC_04";
        server.expect(requestTo("/BuC?BuCType=" + buc))
            .andRespond(withSuccess("1234", MediaType.APPLICATION_JSON));

        String response = euxConsumer.opprettBuC(buc);
        assertEquals(id, response);
    }

    @Test
    public void slettBuC_ingenRetur() throws MelosysException {
        String id = "1234";
        server.expect(requestTo("/BuC?RINASaksnummer=" + id))
            .andRespond(withSuccess());

        euxConsumer.slettBuC(id);
    }

    @Test
    public void settMottaker_ingenRetur() throws MelosysException {
        String id = "1234";
        String mottaker = "NAV_DANMARK_123";
        server.expect(requestTo("/BuCDeltagere?RINASaksnummer=" + id + "&MottakerID=" + mottaker))
            .andRespond(withSuccess("1234", MediaType.APPLICATION_JSON));

        euxConsumer.settMottaker(id, mottaker);
    }

    @Test
    public void hentBucTypePerSektor_returnerListe() throws MelosysException, JsonProcessingException {

        List<String> forventetRetur = Lists.newArrayList("en", "to", "tre");

        server.expect(requestTo("/BuCTypePerSektor"))
            .andRespond(withSuccess(objectMapper.writeValueAsString(forventetRetur), MediaType.APPLICATION_JSON));

        List<String> resultat = euxConsumer.bucTypePerSektor();
        assertEquals(resultat, forventetRetur);
    }

    @Test
    public void hentInstitusjoner_forventListe() throws MelosysException, JsonProcessingException {
        List<String> forventetRetur = Lists.newArrayList("en", "to", "tre");
        String buctype = "LA_BUC_04";
        String landkode = "NO";

        server.expect(requestTo("/Institusjoner?BuCType=" + buctype + "&LandKode=" + landkode))
            .andRespond(withSuccess(objectMapper.writeValueAsString(forventetRetur), MediaType.APPLICATION_JSON));

        List<String> resultat = euxConsumer.hentInstitusjoner(buctype, landkode);
        assertEquals(resultat, forventetRetur);
    }

    @Test
    public void hentKodeverk_forventJson() throws MelosysException, JsonProcessingException {
        Map<String, Object> forventetRetur = Maps.newHashMap();
        forventetRetur.put("string", "value");
        forventetRetur.put("int", 1L);

        String kodeverk = "Test";

        server.expect(requestTo("/Kodeverk?Kodeverk=" + kodeverk))
            .andRespond(withSuccess(objectMapper.writeValueAsString(forventetRetur), MediaType.APPLICATION_JSON));

        JsonNode resultat = euxConsumer.hentKodeverk(kodeverk);
        assertTrue(resultat.has("string"));
        assertTrue(resultat.has("int"));
    }

    @Test
    public void hentMuligeAksjoner_forventJson() throws MelosysException, JsonProcessingException {
        Map<String, Object> forventetRetur = Maps.newHashMap();
        forventetRetur.put("string", "value");
        forventetRetur.put("int", 1L);

        String id = "1234";
        server.expect(requestTo("/MuligeAksjoner?RINASaksnummer=" + id))
            .andRespond(withSuccess(objectMapper.writeValueAsString(forventetRetur), MediaType.APPLICATION_JSON));

        JsonNode resultat = euxConsumer.hentMuligeAksjoner(id);
        assertTrue(resultat.has("string"));
        assertTrue(resultat.has("int"));
    }

    @Test
    public void opprettBucOgSed_forventString() throws MelosysException {
        String buc = "buc", fagsak = "123", mottaker = "NAV", filtype = "virus.exe", korrelasjon = "111", vedlegg = "vedlegg";
        SED sed = new SED();

        server.expect(requestTo("/OpprettBuCogSED?BuCType=" + buc + "&FagSakNummer=" + fagsak +
            "&MottakerID=" + mottaker + "&Filtype=" + filtype + "&KorrelasjonsID=" + korrelasjon))
            .andRespond(withSuccess("SUKSESS123", MediaType.APPLICATION_JSON));

        String resultat = euxConsumer.opprettBucOgSed(buc, fagsak, mottaker, filtype, korrelasjon, sed, vedlegg);
        assertNotNull(resultat);
    }

    @Test
    public void finndRinaSaker_forventJson() throws MelosysException, JsonProcessingException {
        Map<String, Object> forventetRetur = Maps.newHashMap();
        forventetRetur.put("string", "value");
        forventetRetur.put("int", 1L);

        String fnr = "123", fornavn = "Andre", etternavn = "Måns", fødselsdato = "12-12-12", saksnummer = "123",
            bucType = "LA_BUC_04", status = "ferdig";

        //Må encode uri, da non-ascii blir escaped
        String uri = UriComponentsBuilder.fromUriString("/RINASaker?Fødselsnummer=" + fnr + "&Fornavn=" + fornavn + "&Etternavn=" + etternavn +
            "&Fødselsdato=" + fødselsdato + "&RINASaksnummer=" + saksnummer + "&BuCType=" + bucType + "&Status=" + status).toUriString();

        server.expect(requestTo(uri))
            .andRespond(withSuccess(objectMapper.writeValueAsString(forventetRetur), MediaType.APPLICATION_JSON));

        JsonNode resultat = euxConsumer.finnRinaSaker(fnr, fornavn, etternavn, fødselsdato, saksnummer, bucType, status);
        assertTrue(resultat.has("string"));
        assertTrue(resultat.has("int"));
    }

    @Test
    public void hentSed_forventSed() throws MelosysException, IOException {
        String id = "123";
        String dokumentId = "312";

        URL jsonUrl = getClass().getClassLoader().getResource("mock/eux/sedA009.json");
        assertNotNull(jsonUrl);
        String sed = IOUtils.toString(jsonUrl);

        server.expect(requestTo("/SED?RINASaksnummer=" + id + "&DokumentID=" + dokumentId))
            .andRespond(withSuccess(sed, MediaType.APPLICATION_JSON));

        SED resultat = euxConsumer.hentSed(id, dokumentId);
        assertNotNull(resultat);
        assertNotNull(resultat.getNav());
        assertEquals("A009", resultat.getSed());
        assertNotNull(resultat.getMedlemskap());
        assertEquals(MedlemskapA009.class, resultat.getMedlemskap().getClass());
    }

    @Test
    public void opprettSed_forventId() throws MelosysException {
        String id = "123";
        String korrelasjonId = "312";
        SED sed = new SED();

        String forventetRetur = "123321";

        server.expect(requestTo("/SED?RINASaksnummer=" + id + "&KorrelasjonsID=" + korrelasjonId))
            .andRespond(withSuccess(forventetRetur, MediaType.APPLICATION_JSON));

        String resultat = euxConsumer.opprettSed(id, korrelasjonId, sed);
        assertEquals(forventetRetur, resultat);
    }

    @Test
    public void oppdaterSed_ingenRetur() throws MelosysException {
        String id = "123";
        String korrelasjonId = "312";
        String sedType = "LA_BUC_04";
        String dokumentId = "1111";
        SED sed = new SED();

        server.expect(requestTo("/SED?RINASaksnummer=" + id + "&KorrelasjonsID=" + korrelasjonId + "&SEDType=" + sedType))
            .andRespond(withSuccess());

        euxConsumer.oppdaterSed(id, korrelasjonId, sedType, dokumentId, sed);
    }

    @Test
    public void slettSed_ingenRetur() throws MelosysException {
        String id = "123";
        String dokumentId = "1122233";

        server.expect(requestTo("/SED?RINASaksnummer=" + id + "&DokumentID=" + dokumentId))
            .andRespond(withSuccess());

        euxConsumer.slettSed(id, dokumentId);
    }

    @Test
    public void sendSed_ingenRetur() throws MelosysException {
        String id = "123";
        String korrelasjonsId = "111";
        String dokumentId = "22";

        server.expect(requestTo("/SendSED?RINASaksnummer=" + id + "&KorrelasjonsID=" + korrelasjonsId +  "&DokumentID=" + dokumentId))
            .andRespond(withSuccess());

        euxConsumer.sendSed(id, korrelasjonsId, dokumentId);
    }

    @Test
    public void hentTilgjengeligeSedType_forventListeString() throws MelosysException, JsonProcessingException {
        String id = "123";

        List<String> forventetRetur = Lists.newArrayList("en", "to", "tre");

        server.expect(requestTo("/TilgjengeligeSEDTyper?RINASaksnummer=" + id))
            .andRespond(withSuccess(objectMapper.writeValueAsString(forventetRetur), MediaType.APPLICATION_JSON));

        List<String> resultat = euxConsumer.hentTilgjengeligeSedTyper(id);
        assertEquals(forventetRetur, resultat);
    }

    @Test
    public void hentVedlegg_ForventByteArray() throws MelosysException {
        String id = "123";
        String dokumentId = "123321";
        String vedleggId = "2222";

        byte[] forventetRetur = "returverdi".getBytes();

        server.expect(requestTo("/Vedlegg?RINASaksnummer=" + id + "&DokumentID=" + dokumentId + "&VedleggID=" + vedleggId))
            .andRespond(withSuccess(forventetRetur, MediaType.APPLICATION_OCTET_STREAM));

        byte[] resultat = euxConsumer.hentVedlegg(id, dokumentId, vedleggId);
        assertTrue(Arrays.equals(forventetRetur, resultat));
    }

    @Test
    public void leggTilVedlegg_forventId() throws MelosysException {
        String id = "123";
        String dokumentId = "123321";
        String filtype = "virus.exe";

        String forventetRetur = "returverdi#123";

        server.expect(requestTo("/Vedlegg?RINASaksnummer=" + id + "&DokumentID=" + dokumentId + "&Filtype=" + filtype))
            .andRespond(withSuccess(forventetRetur, MediaType.APPLICATION_JSON));

        String resultat = euxConsumer.leggTilVedlegg(id, dokumentId, filtype, "vedlegg");
        assertEquals(forventetRetur, resultat);
    }

    @Test
    public void slettVedlegg_ingenRetur() throws MelosysException {
        String id = "123";
        String dokumentId = "123321";
        String vedleggId = "2222";

        server.expect(requestTo("/Vedlegg?RINASaksnummer=" + id + "&DokumentID=" + dokumentId + "&VedleggID=" + vedleggId))
            .andRespond(withSuccess());

        euxConsumer.slettVedlegg(id, dokumentId, vedleggId);
    }

    @Test(expected = MelosysException.class)
    public void exceptionHåndtering_forventMelsoysException() throws MelosysException {
        String id = "123";
        String dokumentId = "123321";
        String vedleggId = "2222";

        server.expect(requestTo("/Vedlegg?RINASaksnummer=" + id + "&DokumentID=" + dokumentId + "&VedleggID=" + vedleggId))
            .andRespond(withBadRequest());

        euxConsumer.slettVedlegg(id, dokumentId, vedleggId);
    }



}
