package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Henleggelsesgrunner;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.sak.*;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.VedleggDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import no.nav.melosys.tjenester.gui.util.SaksbehandlingDataFactory;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FagsakTjeneste.class})
class FagsakTjenesteTest {

    @MockBean
    private static FagsakService fagsakService;
    @MockBean
    private static OpprettNySakFraOppgave opprettNySakFraOppgave;
    @MockBean
    private static Aksesskontroll aksesskontroll;
    @MockBean
    private static HenleggFagsakService henleggFagsakService;
    @MockBean
    private static UtpekingService utpekingService;
    @MockBean
    private static OrganisasjonOppslagService organisasjonOppslagService;
    @MockBean
    private static PersondataFasade persondataFasade;
    @MockBean
    private static VideresendSoknadService videresendSoknadService;
    @MockBean
    private static SaksopplysningerService saksopplysningerService;
    @MockBean
    private static BehandlingsgrunnlagService behandlingsgrunnlagService;
    @MockBean
    private static Unleash unleash;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private EasyRandom random;

    private static final String FNR = "12345678901";
    private static final String ORGNR = "111111111";
    private static final String BASE_URL = "/api/fagsaker";

    @BeforeEach
    void setUp() {
        random = new EasyRandom(new EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .objectPoolSize(100)
            .dateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
            .excludeField(named("tilleggsinformasjonDetaljer").and(ofType(TilleggsinformasjonDetaljer.class)).and(inClass(Tilleggsinformasjon.class)))
            .stringLengthRange(2, 10)
            .randomize(MidlertidigPostadresse.class, () -> Math.random() > 0.5 ? random.nextObject(MidlertidigPostadresseNorge.class) : random.nextObject(MidlertidigPostadresseUtland.class))
            .randomize(named("fnr").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9)));
    }

    @Test
    void hentFagsak() throws Exception {
        Fagsak fagsak = lagFagsak();
        var bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Set.of(bruker));

        lagFagsakTjeneste(fagsak);

        mockMvc.perform(get(BASE_URL + "/{saksnr}", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(lagFagsakDto(fagsak), FagsakDto.class));
    }

    @Test
    void opprettFagsak() throws Exception {
        var opprettSakDto = new OpprettSakDto();
        opprettSakDto.setBrukerID(FNR);

        mockMvc.perform(post(BASE_URL + "/opprett")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto)))
            .andExpect(status().isNoContent());
        verify(aksesskontroll).autoriserFolkeregisterIdent(opprettSakDto.getBrukerID());
    }

    @Test
    void opprettSak_utenFnr_badRequestException() throws Exception {
        lagFagsakTjeneste(null);
        var opprettSakDto = new OpprettSakDto();

        mockMvc.perform(post(BASE_URL + "/opprett")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", equalTo("BrukerID trengs for å opprette en sak.")));
    }

    @Test
    void hentFagsaker_medFnr_verifiserErMappetKorrekt() throws Exception {
        Fagsak fagsak = SaksbehandlingDataFactory.lagFagsak("MEL-1");
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(123L);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        lagFagsakTjeneste(fagsak);
        var fagsakSokDto = new FagsakSokDto(FNR, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo("MEL-1")));
    }

    @Test
    void hentFagsaker_medTomtFnr_verifiserAtNavnErUkjent() throws Exception {
        Fagsak fagsak = lagFagsak();
        var behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Set.of(aktoer));
        lagFagsakTjeneste(fagsak);
        var fagsakSokDto = new FagsakSokDto(FNR, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].navn", equalTo("UKJENT")))
            .andExpect(jsonPath("$[0].saksnummer", equalTo("MEL-1")));
    }

    @Test
    void hentFagsaker_medOrgnr_verifiserErMappetKorrekt() throws Exception {
        Fagsak fagsak = lagFagsak();
        var behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        Aktoer aktoer = new Aktoer();
        aktoer.setOrgnr(ORGNR);
        aktoer.setRolle(Aktoersroller.VIRKSOMHET);
        fagsak.setAktører(Set.of(aktoer));
        lagFagsakTjeneste(fagsak);
        var organisajonsdokument = new OrganisasjonDokument();
        organisajonsdokument.setNavn("Moe Organisasjon");
        when(organisasjonOppslagService.hentOrganisasjon(ORGNR)).thenReturn(organisajonsdokument);
        var fagsakSokDto = new FagsakSokDto(null, null, ORGNR);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.VIRKSOMHET.toString())))
            .andExpect(jsonPath("$[0].navn", equalTo("Moe Organisasjon")));
    }

    @Test
    void hentFagsaker_medTomtOrgnr_verifiserAtNavnErUkjent() throws Exception {
        Fagsak fagsak = lagFagsak();
        var behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        var aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.VIRKSOMHET);
        fagsak.setAktører(Set.of(aktoer));
        lagFagsakTjeneste(fagsak);
        var fagsakSokDto = new FagsakSokDto(null, null, ORGNR);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.VIRKSOMHET.toString())))
            .andExpect(jsonPath("$[0].navn", equalTo("UKJENT")));
    }

    @Test
    void hentFagsaker_medSaksnummer_finnerIkkeSakMottarTomListe() throws Exception {
        Fagsak fagsak = lagFagsak();
        lagFagsakTjeneste(fagsak);
        var fagsakSokDto = new FagsakSokDto(null, "NEI-123", null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", equalTo(0)));
    }

    @Test
    void henleggFagsak() throws Exception {
        String begrunnelseKode = Henleggelsesgrunner.OPPHOLD_UTL_AVLYST.getKode();
        String fritekst = "Dette er fritekst";
        var henleggelseDto = new HenleggelseDto(fritekst, begrunnelseKode);
        Fagsak fagsak = lagFagsak();
        lagFagsakTjeneste(fagsak);
        String saksnummer = "123";

        mockMvc.perform(post(BASE_URL + "/{saksnr}/henlegg", saksnummer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(henleggelseDto)))
            .andExpect(status().isNoContent());

        verify(henleggFagsakService).henleggFagsakEllerBehandling(saksnummer, begrunnelseKode, fritekst);
    }

    @Test
    void videresend() throws Exception {
        var videresendDto = new VideresendDto();
        videresendDto.setVedlegg(Collections.singletonList(new VedleggDto("1", "2")));
        videresendDto.setMottakerinstitusjon("Denmark");
        videresendDto.setFritekst("Dette er fritekst");
        Fagsak fagsak = lagFagsak();
        lagFagsakTjeneste(fagsak);

        mockMvc.perform(post(BASE_URL + "/{saksnr}/henlegg-videresend", "123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(videresendDto)))
            .andExpect(status().isNoContent());
    }

    @Test
    void videresend_utenVedlegg_feiler() throws Exception {
        Fagsak fagsak = lagFagsak();
        lagFagsakTjeneste(fagsak);
        var videresendDto = new VideresendDto();

        mockMvc.perform(post(BASE_URL + "/{saksnr}/henlegg-videresend", "123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(videresendDto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("uten vedlegg")));
    }

    @Test
    void henleggSakSomBortfalt() throws Exception {
        Fagsak fagsak = lagFagsak();
        lagFagsakTjeneste(fagsak);
        String saksnummer = "123";
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);

        mockMvc.perform(put(BASE_URL + "/{saksnr}/henlegg-som-bortfalt", saksnummer)
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isNoContent());

        verify(henleggFagsakService).henleggSakEllerBehandlingSomBortfalt(saksnummer);
    }

    @Test
    void avsluttSakManuelt() throws Exception {
        Fagsak fagsak = lagFagsak();
        lagFagsakTjeneste(fagsak);

        mockMvc.perform(put(BASE_URL + "/{saksnr}/avslutt", "123")
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isNoContent());

        verify(fagsakService).avsluttFagsakOgBehandlingValiderBehandlingstype(fagsak, fagsak.hentAktivBehandling());
    }

    @Test
    void utpekLovvalgsland() throws Exception {
        Fagsak fagsak = lagFagsak();
        lagFagsakTjeneste(fagsak);
        UtpekDto utpekDto = new UtpekDto(Set.of("SE:123"), "Fri SED", "Fri brev");
        when(fagsakService.hentFagsak(any())).thenReturn(fagsak);

        mockMvc.perform(post(BASE_URL + "/{saksnr}/utpek", "123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(utpekDto)))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSakstilgang(fagsak);
        verify(utpekingService).utpekLovvalgsland(fagsak, utpekDto.mottakerinstitusjoner(), utpekDto.fritekstSed(), utpekDto.fritekstBrev());
    }

    @Test
    void revurderSisteBehandling() throws Exception {
        Fagsak fagsak = lagFagsak();
        lagFagsakTjeneste(fagsak);
        when(fagsakService.opprettNyVurderingBehandling("123")).thenReturn(1L);

        mockMvc.perform(post(BASE_URL + "/{saksnr}/revurder", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.behandlingID", equalTo(1)));

        verify(aksesskontroll).autoriserSakstilgang("123");
    }

    private static FagsakTjeneste lagFagsakTjeneste(Fagsak fagsak) {
        Soeknad søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument();
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknadDokument);
        when(behandlingsgrunnlagService.finnBehandlingsgrunnlag(1L)).thenReturn(Optional.of(behandlingsgrunnlag));
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        when(persondataFasade.hentSammensattNavn(any())).thenReturn("Joe Moe");
        if (fagsak != null) {
            doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedAktør(Aktoersroller.BRUKER, FNR);
            doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, ORGNR);
        }
        return new FagsakTjeneste(fagsakService, aksesskontroll, behandlingsgrunnlagService, henleggFagsakService, opprettNySakFraOppgave,
            persondataFasade, saksopplysningerService, utpekingService, videresendSoknadService, organisasjonOppslagService);
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");
        fagsak.setBehandlinger(Collections.emptyList());
        return fagsak;
    }

    private static FagsakDto lagFagsakDto(Fagsak fagsak) {
        FagsakDto resultat = new FagsakDto();
        resultat.setEndretDato(fagsak.getEndretDato());
        resultat.setGsakSaksnummer(fagsak.getGsakSaksnummer());
        resultat.setRegistrertDato(fagsak.getRegistrertDato());
        resultat.setSaksnummer(fagsak.getSaksnummer());
        resultat.setSakstype(fagsak.getType());
        resultat.setSaksstatus(fagsak.getStatus());
        resultat.setHovedpartRolle(fagsak.getHovedpartRolle());
        return resultat;
    }
}
