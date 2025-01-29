package no.nav.melosys.tjenester.gui.fagsaker;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService;
import no.nav.melosys.service.sak.*;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import no.nav.melosys.tjenester.gui.dto.FagsakSokDto;
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
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

import static no.nav.melosys.domain.FagsakTestFactory.*;
import static org.hamcrest.Matchers.*;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FagsakController.class})
class FagsakControllerTest {

    private static final String BASE_URL = "/api/fagsaker";
    private static final LocalDate FOM = LocalDate.now();
    private static final LocalDate TOM = LocalDate.now();
    private static final LocalDate MOTTAKSDATO = LocalDate.now();
    private static final LovvalgsperiodeDto FORVENTET_LOVVALGSPERIODE = new LovvalgsperiodeDto(
        "1L", new PeriodeDto(FOM, TOM),
        Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2,
        Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1,
        Land_iso2.SK,
        InnvilgelsesResultat.AVSLAATT,
        Trygdedekninger.FULL_DEKNING_EOSFO,
        Medlemskapstyper.FRIVILLIG,
        "10");

    @MockBean
    private static FagsakService fagsakService;
    @MockBean
    private static OpprettSak opprettSak;
    @MockBean
    private static EndreSakService endreSakService;
    @MockBean
    private static Aksesskontroll aksesskontroll;
    @MockBean
    private static OrganisasjonOppslagService organisasjonOppslagService;
    @MockBean
    private static PersondataFasade persondataFasade;
    @MockBean
    @SuppressWarnings("unused")
    private static SaksopplysningerService saksopplysningerService;
    @MockBean
    private static MottatteOpplysningerService mottatteOpplysningerService;
    @MockBean
    private static BehandlingsresultatService behandlingsresultatService;
    @MockBean
    @SuppressWarnings("unused")
    private static OpprettBehandlingForSak opprettBehandlingForSak;
    @MockBean
    private static FerdigbehandleService ferdigbehandleService;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    private EasyRandom random;

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
        Fagsak fagsak = FagsakTestFactory.builder().medBruker().build();
        when(fagsakService.hentFagsak(SAKSNUMMER)).thenReturn(fagsak);

        var expectedResponse = lagFagsakDto(fagsak);
        mockMvc.perform(get(BASE_URL + "/{saksnr}", SAKSNUMMER)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("saksnummer", equalTo(expectedResponse.getSaksnummer())))
            .andExpect(jsonPath("gsakSaksnummer", equalTo(expectedResponse.getGsakSaksnummer())))
            .andExpect(jsonPath("sakstema.kode", equalTo(expectedResponse.getSakstema().getKode())))
            .andExpect(jsonPath("sakstype.kode", equalTo(expectedResponse.getSakstype().getKode())))
            .andExpect(jsonPath("saksstatus.kode", equalTo(expectedResponse.getSaksstatus().getKode())))
            .andExpect(jsonPath("registrertDato", equalTo(expectedResponse.getRegistrertDato().toString())))
            .andExpect(jsonPath("endretDato", equalTo(expectedResponse.getEndretDato().toString())))
            .andExpect(jsonPath("hovedpartRolle", equalTo(expectedResponse.getHovedpartRolle().toString())));
    }

    @Test
    void opprettFagsak() throws Exception {
        var opprettSakDto = new OpprettSakDto();
        opprettSakDto.setBrukerID(BRUKER_AKTØR_ID);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto)))
            .andExpect(status().isNoContent());
        verify(aksesskontroll).autoriserFolkeregisterIdent(opprettSakDto.getBrukerID());
        verify(opprettSak).opprettNySakOgBehandling(any(OpprettSakDto.class));
    }

    @Test
    void opprettSak_utenFnrEllerOrgnr_badRequestException() throws Exception {
        var opprettSakDto = new OpprettSakDto();
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void lagNyBehandling() throws Exception {
        Fagsak fagsak = SaksbehandlingDataFactory.lagFagsak();
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(123L);

        fagsak.leggTilBehandling(behandling);
        var opprettSakDto = new OpprettSakDto();
        opprettSakDto.setBrukerID(BRUKER_AKTØR_ID);
        opprettSakDto.setBehandlingstema(Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY);
        opprettSakDto.setBehandlingstype(Behandlingstyper.NY_VURDERING);

        mockMvc.perform(post(BASE_URL + "/{saksnr}/behandlinger", fagsak.getSaksnummer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto)))
            .andExpect(status().isNoContent());
        verify(aksesskontroll).autoriserFolkeregisterIdent(opprettSakDto.getBrukerID());
    }

    @Test
    void hentFagsaker_medFnr_verifiserErMappetKorrekt() throws Exception {
        Fagsak fagsak = SaksbehandlingDataFactory.lagFagsak();
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(123L);
        fagsak.leggTilBehandling(behandling);
        mockFagsakController(fagsak, null);
        var fagsakSokDto = new FagsakSokDto(BRUKER_AKTØR_ID, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo(SAKSNUMMER)));
    }

    @Test
    void hentFagsaker_medBehandlingsresultatOgLovvalgsperiode_verifiserErMappetKorrekt() throws Exception {
        long behandlingID = 123L;

        mockFagsakMedBehandling(behandlingID);

        var fagsakSokDto = new FagsakSokDto(BRUKER_AKTØR_ID, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo(SAKSNUMMER)))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", equalTo(Landkoder.DK.getKode())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", is(not(equalTo(FORVENTET_LOVVALGSPERIODE.lovvalgsland)))))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.fom", equalTo("2019-01-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.tom", equalTo("2019-02-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].lovvalgsperiode.fom", equalTo(FORVENTET_LOVVALGSPERIODE.periode.getFom().toString())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].lovvalgsperiode.tom", equalTo(FORVENTET_LOVVALGSPERIODE.periode.getTom().toString())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].medlemskapsperiode.fom", equalTo(null))) //TODO: Her burde vi kanskje returnere null på medlemskapsperiode?
            .andExpect(jsonPath("$[0].behandlingOversikter[0].medlemskapsperiode.tom", equalTo(null))); //TODO: Her burde vi kanskje returnere null på medlemskapsperiode?
    }

    @Test
    void hentFagsaker_medMedlemAvFolketrygdenOgMedlemskapsperioder_verifiserErMappetKorrekt() throws Exception {
        long behandlingID = 123L;

        var fagsak = SaksbehandlingDataFactory.lagFagsak();
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(behandlingID);
        fagsak.leggTilBehandling(behandling);
        var behandlingsresultat = new Behandlingsresultat();
        var medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setFom(FOM);
        medlemskapsperiode.setTom(TOM);
        medlemskapsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setMedlemskapsperioder(List.of(medlemskapsperiode));
        mockFagsakController(fagsak, behandlingsresultat);

        var fagsakSokDto = new FagsakSokDto(BRUKER_AKTØR_ID, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo(SAKSNUMMER)))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", equalTo(Landkoder.DK.getKode())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", is(not(equalTo(FORVENTET_LOVVALGSPERIODE.lovvalgsland)))))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.fom", equalTo("2019-01-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.tom", equalTo("2019-02-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].lovvalgsperiode", equalTo(null)))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].medlemskapsperiode.fom", equalTo(FOM.toString())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].medlemskapsperiode.tom", equalTo(TOM.toString())));
    }

    @Test
    void hentFagsaker_medTomtFnr_verifiserAtNavnErUkjent() throws Exception {
        Aktoer brukerUtenFnr = new Aktoer();
        brukerUtenFnr.setRolle(Aktoersroller.BRUKER);
        Fagsak fagsak = FagsakTestFactory.builder().aktører(brukerUtenFnr).build();
        var behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.leggTilBehandling(behandling);
        mockFagsakController(fagsak, null);
        var fagsakSokDto = new FagsakSokDto(BRUKER_AKTØR_ID, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].navn", equalTo("UKJENT")))
            .andExpect(jsonPath("$[0].saksnummer", equalTo(SAKSNUMMER)));
    }

    @Test
    void hentFagsaker_medOrgnr_verifiserErMappetKorrekt() throws Exception {
        Fagsak fagsak = FagsakTestFactory.builder().medVirksomhet().build();
        var behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.leggTilBehandling(behandling);
        mockFagsakController(fagsak, null);

        var organisajonsdokument = OrganisasjonDokumentTestFactory.builder()
            .navn("Moe Organisasjon")
            .build();
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
        var aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.VIRKSOMHET);
        Fagsak fagsak = FagsakTestFactory.builder().aktører(aktoer).build();
        var behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.leggTilBehandling(behandling);
        mockFagsakController(fagsak, null);
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
        var fagsakSokDto = new FagsakSokDto(null, "NEI-123", null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", equalTo(0)));
    }

    @Test
    void endreSak() throws Exception {
        EndreSakDto endreSakDto = new EndreSakDto(null, Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET, Behandlingstyper.NY_VURDERING, Behandlingsstatus.OPPRETTET, null);

        mockMvc.perform(put(BASE_URL + "/{saksnr}", SAKSNUMMER)
                .content(objectMapper.writeValueAsString(endreSakDto))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSakstilgang(SAKSNUMMER);
        verify(endreSakService).endre(SAKSNUMMER, Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET, Behandlingstyper.NY_VURDERING, Behandlingsstatus.OPPRETTET, null);
    }

    @Test
    void endreÅrsavregningOppsummering() throws Exception {
        EndreSakDto endreSakDto = new EndreSakDto(BEHANDLING_ID, Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV, Behandlingstyper.ÅRSAVREGNING, Behandlingsstatus.UNDER_BEHANDLING, MOTTAKSDATO);

        mockMvc.perform(put(BASE_URL + "/{saksnr}", SAKSNUMMER)
                .content(objectMapper.writeValueAsString(endreSakDto))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSakstilgang(SAKSNUMMER);
        verify(endreSakService).endreÅrsavregningOppsummering(BEHANDLING_ID, Behandlingsstatus.UNDER_BEHANDLING, MOTTAKSDATO);
    }

    @Test
    void ferdigbehandleSak() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{behandlingID}/ferdigbehandle", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSkriv(BEHANDLING_ID);
        verify(ferdigbehandleService).ferdigbehandle(BEHANDLING_ID);
    }

    private void mockFagsakController(Fagsak fagsak, Behandlingsresultat eksisterendeBehres) {
        Soeknad søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument();
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(søknadDokument);
        Behandlingsresultat nyttBehres = new Behandlingsresultat();
        nyttBehres.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        nyttBehres.getLovvalgsperioder().add(lagLovvalgsPeriode());
        Behandlingsresultat behandlingsresultat = eksisterendeBehres == null ? nyttBehres : eksisterendeBehres;

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        when(behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(anyLong())).thenReturn(behandlingsresultat);
        when(mottatteOpplysningerService.finnMottatteOpplysninger(fagsak.getBehandlinger().get(0).getId())).thenReturn(Optional.of(mottatteOpplysninger));
        when(fagsakService.hentFagsak(SAKSNUMMER)).thenReturn(fagsak);
        when(persondataFasade.hentSammensattNavn(any())).thenReturn("Joe Moe");
        doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedAktør(Aktoersroller.BRUKER, BRUKER_AKTØR_ID);
        doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, ORGNR);
    }

    private void mockFagsakMedBehandling(long behandlingID) {
        var fagsak = SaksbehandlingDataFactory.lagFagsak();
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(behandlingID);
        fagsak.leggTilBehandling(behandling);
        mockFagsakController(fagsak, null);
    }

    private Lovvalgsperiode lagLovvalgsPeriode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(FORVENTET_LOVVALGSPERIODE.periode.getFom());
        lovvalgsperiode.setTom(FORVENTET_LOVVALGSPERIODE.periode.getTom());
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        lovvalgsperiode.setLovvalgsland(Land_iso2.valueOf(FORVENTET_LOVVALGSPERIODE.lovvalgsland));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.valueOf(FORVENTET_LOVVALGSPERIODE.lovvalgsbestemmelse));
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.valueOf(FORVENTET_LOVVALGSPERIODE.tilleggBestemmelse));
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.valueOf(FORVENTET_LOVVALGSPERIODE.innvilgelsesResultat));
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.valueOf(FORVENTET_LOVVALGSPERIODE.medlemskapstype));
        lovvalgsperiode.setMedlPeriodeID(Long.valueOf(FORVENTET_LOVVALGSPERIODE.medlemskapsperiodeID));

        return lovvalgsperiode;
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
