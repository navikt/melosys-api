package no.nav.melosys.tjenester.gui.sak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.finn.unleash.Unleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.sak.OpprettBehandlingForSak;
import no.nav.melosys.service.sak.OpprettSak;
import no.nav.melosys.service.sak.OpprettSakDto;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import no.nav.melosys.tjenester.gui.dto.FagsakSokDto;
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
import static org.hamcrest.Matchers.equalTo;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FagsakTjeneste.class})
class FagsakTjenesteTest {

    private static final String FNR = "12345678901";
    private static final String ORGNR = "111111111";
    private static final String BASE_URL = "/api/fagsaker";
    @MockBean
    private static FagsakService fagsakService;
    @MockBean
    private static OpprettSak opprettSak;
    @MockBean
    private static Aksesskontroll aksesskontroll;
    @MockBean
    private static OrganisasjonOppslagService organisasjonOppslagService;
    @MockBean
    private static PersondataFasade persondataFasade;
    @MockBean
    private static SaksopplysningerService saksopplysningerService;
    @MockBean
    private static BehandlingsgrunnlagService behandlingsgrunnlagService;
    @MockBean
    private static BehandlingsresultatService behandlingsresultatService;
    @MockBean
    private static OpprettBehandlingForSak opprettBehandlingForSak;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private Unleash unleash;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        when(unleash.isEnabled("melosys.behandle_alle_saker")).thenReturn(true);

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

        mockFagsakTjeneste(fagsak);

        mockMvc.perform(get(BASE_URL + "/{saksnr}", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(lagFagsakDto(fagsak), FagsakDto.class));
    }

    @Test
    void opprettFagsak() throws Exception {
        var opprettSakDto = new OpprettSakDto();
        opprettSakDto.setBrukerID(FNR);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto)))
            .andExpect(status().isNoContent());
        verify(aksesskontroll).autoriserFolkeregisterIdent(opprettSakDto.getBrukerID());
    }

    @Test
    void opprettSak_utenFnrEllerOrgnr_badRequestException() throws Exception {
        mockFagsakTjeneste(null);
        var opprettSakDto = new OpprettSakDto();
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void lagNyBehandling() throws Exception {
        Fagsak fagsak = SaksbehandlingDataFactory.lagFagsak("MEL-1");
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(123L);

        fagsak.setBehandlinger(Collections.singletonList(behandling));
        var opprettSakDto = new OpprettSakDto();
        opprettSakDto.setBrukerID(FNR);
        opprettSakDto.setBehandlingstema(Behandlingstema.ARBEID_ETT_LAND_ØVRIG);
        opprettSakDto.setBehandlingstype(Behandlingstyper.NY_VURDERING);

        mockMvc.perform(post(BASE_URL + "/{saksnr}/behandlinger", fagsak.getSaksnummer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto)))
            .andExpect(status().isNoContent());
        verify(aksesskontroll).autoriserFolkeregisterIdent(opprettSakDto.getBrukerID());
    }

    @Test
    void hentFagsaker_medFnr_verifiserErMappetKorrekt() throws Exception {
        Fagsak fagsak = SaksbehandlingDataFactory.lagFagsak("MEL-1");
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(123L);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        mockFagsakTjeneste(fagsak);
        var fagsakSokDto = new FagsakSokDto(FNR, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo("MEL-1")));
    }

    @Test
    void hentFagsaker_medSedDokument_verifiserErMappetKorrekt() throws Exception {
        long behandlingID = 123L;

        var sedDokumentFom = LocalDate.of(2022, 1, 1);
        var sedDokumentTom = LocalDate.of(2022, 2, 1);
        mockNorskSedDokumentMedPeriode(behandlingID, sedDokumentFom, sedDokumentTom);

        String saksnummer = "MEL-1";
        mockFagsakMedBehandling(behandlingID, saksnummer);

        var fagsakSokDto = new FagsakSokDto(FNR, null, null);


        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo(saksnummer)))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", equalTo("NO")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].periode.fom", equalTo("2022-01-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].periode.tom", equalTo("2022-02-01")));

        verify(saksopplysningerService).finnSedOpplysninger(behandlingID);
    }

    @Test
    void hentFagsaker_medBehandlingsgrunnlag_verifiserBehandlingsgrunnlagPeriodeErMappetKorrekt() throws Exception {
        var behandlingID = 123L;
        when(saksopplysningerService.finnSedOpplysninger(behandlingID)).thenReturn(Optional.empty());

        var søknadFom = LocalDate.of(2023, 1, 1);
        var søknadTom = LocalDate.of(2023, 2, 1);
        mockBehandlingsgrunnlagMedPeriode(behandlingID, søknadFom, søknadTom);

        mockFagsakMedBehandling(behandlingID, "MEL-1");

        var fagsakSokDto = new FagsakSokDto(FNR, null, null);


        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].behandlingOversikter[0].periode.fom", equalTo("2023-01-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].periode.tom", equalTo("2023-02-01")));
    }

    @Test
    void hentFagsaker_medSedDokumentOgBehandlingsgrunnlag_verifiserSedDokumentPeriodeBrukes() throws Exception {
        var behandlingID = 123L;

        var sedDokumentFom = LocalDate.of(2022, 1, 1);
        var sedDokumentTom = LocalDate.of(2022, 2, 1);
        mockNorskSedDokumentMedPeriode(behandlingID, sedDokumentFom, sedDokumentTom);

        var søknadFom = LocalDate.of(2066, 1, 1);
        var søknadTom = LocalDate.of(2067, 2, 1);
        mockBehandlingsgrunnlagMedPeriode(behandlingID, søknadFom, søknadTom);

        mockFagsakMedBehandling(behandlingID, "MEL-1");

        var fagsakSokDto = new FagsakSokDto(FNR, null, null);


        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].behandlingOversikter[0].periode.fom", equalTo("2022-01-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].periode.tom", equalTo("2022-02-01")));
    }

    @Test
    void hentFagsaker_medSedDokument_unleashFalse_verifiserMappetKorrektMedGammelLogikk() throws Exception {
        when(unleash.isEnabled("melosys.behandle_alle_saker")).thenReturn(false);
        LocalDate sedDokumentFom = LocalDate.of(2022, 1, 1);
        LocalDate sedDokumentTom = LocalDate.of(2022, 2, 1);
        mockNorskSedDokumentMedPeriode(123L, sedDokumentFom, sedDokumentTom);

        Fagsak fagsak = SaksbehandlingDataFactory.lagFagsak("MEL-1");
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(123L);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        mockFagsakTjeneste(fagsak);
        var fagsakSokDto = new FagsakSokDto(FNR, null, null);


        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo("MEL-1")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", equalTo("NO")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].periode.fom", equalTo("2022-01-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].periode.tom", equalTo("2022-02-01")));

        verify(saksopplysningerService).finnSedOpplysninger(123L);
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
        mockFagsakTjeneste(fagsak);
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
        mockFagsakTjeneste(fagsak);
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
        mockFagsakTjeneste(fagsak);
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
        mockFagsakTjeneste(fagsak);
        var fagsakSokDto = new FagsakSokDto(null, "NEI-123", null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", equalTo(0)));
    }

    @Test
    void revurderSisteBehandling() throws Exception {
        Fagsak fagsak = lagFagsak();
        mockFagsakTjeneste(fagsak);
        when(fagsakService.opprettNyVurderingBehandling("123")).thenReturn(1L);

        mockMvc.perform(post(BASE_URL + "/{saksnr}/revurder", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.behandlingID", equalTo(1)));

        verify(aksesskontroll).autoriserSakstilgang("123");
    }

    @Test
    void ferdigbehandleSak() throws Exception {
        Fagsak fagsak = lagFagsak();
        mockFagsakTjeneste(fagsak);

        mockMvc.perform(put(BASE_URL + "/{saksnr}/ferdigbehandle", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSakstilgang("123");
        verify(fagsakService).ferdigbehandleSak("123");
    }

    private void mockFagsakTjeneste(Fagsak fagsak) {
        Soeknad søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument();
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknadDokument);
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        when(behandlingsgrunnlagService.finnBehandlingsgrunnlag(1L)).thenReturn(Optional.of(behandlingsgrunnlag));
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        when(persondataFasade.hentSammensattNavn(any())).thenReturn("Joe Moe");
        if (fagsak != null) {
            doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedAktør(Aktoersroller.BRUKER, FNR);
            doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, ORGNR);
        }
    }

    private void mockNorskSedDokumentMedPeriode(long behandlingID, LocalDate fom, LocalDate tom) {
        var sedDokument = new SedDokument();
        sedDokument.setLovvalgslandKode(Landkoder.NO);

        var periode = new Periode(fom, tom);
        sedDokument.setLovvalgsperiode(periode);

        when(saksopplysningerService.finnSedOpplysninger(behandlingID)).thenReturn(Optional.of(sedDokument));
    }

    private void mockBehandlingsgrunnlagMedPeriode(long behandlingID, LocalDate søknadFom, LocalDate søknadTom) {
        var søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument();
        søknadDokument.periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(søknadFom, søknadTom);

        var behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(søknadDokument);

        var behandlingsresultat = new Behandlingsresultat();

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        when(behandlingsgrunnlagService.finnBehandlingsgrunnlag(behandlingID)).thenReturn(Optional.of(behandlingsgrunnlag));
    }

    private void mockFagsakMedBehandling(long behandlingID, String saksnummer) {
        var fagsak = SaksbehandlingDataFactory.lagFagsak(saksnummer);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(behandlingID);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        mockFagsakTjeneste(fagsak);


    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");
        fagsak.setBehandlinger(Collections.emptyList());
        return fagsak;
    }

    private FagsakDto lagFagsakDto(Fagsak fagsak) {
        FagsakDto resultat = new FagsakDto();
        resultat.setEndretDato(fagsak.getEndretDato());
        resultat.setGsakSaksnummer(fagsak.getGsakSaksnummer());
        resultat.setRegistrertDato(fagsak.getRegistrertDato());
        resultat.setSaksnummer(fagsak.getSaksnummer());
        resultat.setSakstema(fagsak.getTema());
        resultat.setSakstype(fagsak.getType());
        resultat.setSaksstatus(fagsak.getStatus());
        resultat.setHovedpartRolle(fagsak.getHovedpartRolle());
        return resultat;
    }
}
