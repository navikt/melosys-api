package no.nav.melosys.tjenester.gui.fagsaker;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.OrganisasjonDokumentTestFactory;
import no.nav.melosys.service.MedlemAvFolketrygdenService;
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

import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.hamcrest.Matchers.*;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {FagsakTjeneste.class})
class FagsakTjenesteTest {

    private static final String FNR = "12345678901";
    private static final String ORGNR = "111111111";
    private static final String BASE_URL = "/api/fagsaker";
    private static final LocalDate FOM = LocalDate.now();
    private static final LocalDate TOM = LocalDate.now();
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
    private static MedlemAvFolketrygdenService medlemAvFolketrygdenService;
    @MockBean
    private static FerdigbehandleSakService ferdigbehandleSakService;

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
        Fagsak fagsak = lagFagsak();
        var bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Set.of(bruker));
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);

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
        Fagsak fagsak = SaksbehandlingDataFactory.lagFagsak("MEL-1");
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(123L);

        fagsak.setBehandlinger(Collections.singletonList(behandling));
        var opprettSakDto = new OpprettSakDto();
        opprettSakDto.setBrukerID(FNR);
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
        Fagsak fagsak = SaksbehandlingDataFactory.lagFagsak("MEL-1");
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(123L);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        mockFagsakTjeneste(fagsak, null);
        var fagsakSokDto = new FagsakSokDto(FNR, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo("MEL-1")));
    }

    @Test
    void hentFagsaker_medBehandlingsresultatOgLovvalgsperiode_verifiserErMappetKorrekt() throws Exception {
        long behandlingID = 123L;

        String saksnummer = "MEL-1";
        mockFagsakMedBehandling(behandlingID, saksnummer);

        var fagsakSokDto = new FagsakSokDto(FNR, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo(saksnummer)))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", equalTo(Landkoder.DK.getKode())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", is(not(equalTo(FORVENTET_LOVVALGSPERIODE.getLovvalgsland())))))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.fom", equalTo("2019-01-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.tom", equalTo("2019-02-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].lovvalgsperiode.fom", equalTo(FORVENTET_LOVVALGSPERIODE.getPeriode().getFom().toString())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].lovvalgsperiode.tom", equalTo(FORVENTET_LOVVALGSPERIODE.getPeriode().getTom().toString())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].medlemskapsperiode", equalTo(null)));
    }

    @Test
    void hentFagsaker_medMedlemAvFolketrygdenOgMedlemskapsperioder_verifiserErMappetKorrekt() throws Exception {
        long behandlingID = 123L;
        String saksnummer = "MEL-1";

        var fagsak = SaksbehandlingDataFactory.lagFagsak(saksnummer);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(behandlingID);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setMedlemAvFolketrygden(new MedlemAvFolketrygden());
        var medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setFom(FOM);
        medlemskapsperiode.setTom(TOM);
        medlemskapsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getMedlemAvFolketrygden().setMedlemskapsperioder(List.of(medlemskapsperiode));
        mockFagsakTjeneste(fagsak, behandlingsresultat);

        var fagsakSokDto = new FagsakSokDto(FNR, null, null);

        mockMvc.perform(post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath("$[0].saksnummer", equalTo(saksnummer)))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", equalTo(Landkoder.DK.getKode())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].land.landkoder[0]", is(not(equalTo(FORVENTET_LOVVALGSPERIODE.getLovvalgsland())))))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.fom", equalTo("2019-01-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.tom", equalTo("2019-02-01")))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].lovvalgsperiode", equalTo(null)))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].medlemskapsperiode.fom", equalTo(FOM.toString())))
            .andExpect(jsonPath("$[0].behandlingOversikter[0].medlemskapsperiode.tom", equalTo(TOM.toString())));
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
        mockFagsakTjeneste(fagsak, null);
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
        mockFagsakTjeneste(fagsak, null);

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
        Fagsak fagsak = lagFagsak();
        var behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(List.of(behandling));
        var aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.VIRKSOMHET);
        fagsak.setAktører(Set.of(aktoer));
        mockFagsakTjeneste(fagsak, null);
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
        EndreSakDto endreSakDto = new EndreSakDto(Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET, Behandlingstyper.NY_VURDERING, Behandlingsstatus.OPPRETTET, null);

        mockMvc.perform(post(BASE_URL + "/{saksnr}/endre", "123")
                .content(objectMapper.writeValueAsString(endreSakDto))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSakstilgang("123");
        verify(endreSakService).endre("123", Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET, Behandlingstyper.NY_VURDERING, Behandlingsstatus.OPPRETTET, null);
    }

    @Test
    void ferdigbehandleSak() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{saksnr}/ferdigbehandle", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(aksesskontroll).autoriserSakstilgang("123");
        verify(ferdigbehandleSakService).ferdigbehandleSak("123");
    }

    private void mockFagsakTjeneste(Fagsak fagsak, Behandlingsresultat eksisterendeBehres) {
        Soeknad søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument();
        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(søknadDokument);
        Behandlingsresultat nyttBehres = new Behandlingsresultat();
        nyttBehres.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        nyttBehres.getLovvalgsperioder().add(lagLovvalgsPeriode());
        Behandlingsresultat behandlingsresultat = eksisterendeBehres == null ? nyttBehres : eksisterendeBehres;

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        when(behandlingsresultatService.hentBehandlingsresultatMedLovvalgsperioder(anyLong())).thenReturn(behandlingsresultat);
        when(mottatteOpplysningerService.finnMottatteOpplysninger(fagsak.getBehandlinger().get(0).getId())).thenReturn(Optional.of(mottatteOpplysninger));
        when(medlemAvFolketrygdenService.finnMedlemAvFolketrygdenMedMedlemskapsperioder(anyLong())).thenReturn(Optional.ofNullable(behandlingsresultat.getMedlemAvFolketrygden()));
        when(fagsakService.hentFagsak("123")).thenReturn(fagsak);
        when(persondataFasade.hentSammensattNavn(any())).thenReturn("Joe Moe");
        doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedAktør(Aktoersroller.BRUKER, FNR);
        doReturn(List.of(fagsak)).when(fagsakService).hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, ORGNR);
    }

    private void mockFagsakMedBehandling(long behandlingID, String saksnummer) {
        var fagsak = SaksbehandlingDataFactory.lagFagsak(saksnummer);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setId(behandlingID);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        mockFagsakTjeneste(fagsak, null);
    }

    private Lovvalgsperiode lagLovvalgsPeriode() {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(FORVENTET_LOVVALGSPERIODE.getPeriode().getFom());
        lovvalgsperiode.setTom(FORVENTET_LOVVALGSPERIODE.getPeriode().getTom());
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO);
        lovvalgsperiode.setLovvalgsland(FORVENTET_LOVVALGSPERIODE.getLovvalgsland());
        lovvalgsperiode.setBestemmelse(FORVENTET_LOVVALGSPERIODE.getLovvalgsbestemmelse());
        lovvalgsperiode.setTilleggsbestemmelse(FORVENTET_LOVVALGSPERIODE.getTilleggBestemmelse());
        lovvalgsperiode.setInnvilgelsesresultat(FORVENTET_LOVVALGSPERIODE.getInnvilgelsesResultat());
        lovvalgsperiode.setMedlemskapstype(FORVENTET_LOVVALGSPERIODE.getMedlemskapstype());
        lovvalgsperiode.setMedlPeriodeID(Long.valueOf(FORVENTET_LOVVALGSPERIODE.getMedlemskapsperiodeID()));

        return lovvalgsperiode;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");
        fagsak.setBehandlinger(Collections.emptyList());
        return fagsak;
    }

    private FagsakDto lagFagsakDto(Fagsak fagsak) {
        FagsakDto resultat = new FagsakDto();
        resultat.endretDato = fagsak.getEndretDato();
        resultat.gsakSaksnummer = fagsak.getGsakSaksnummer();
        resultat.registrertDato = fagsak.getRegistrertDato();
        resultat.saksnummer = fagsak.getSaksnummer();
        resultat.sakstema = fagsak.getTema();
        resultat.sakstype = fagsak.getType();
        resultat.saksstatus = fagsak.getStatus();
        resultat.hovedpartRolle = fagsak.getHovedpartRolle();
        return resultat;
    }
}
