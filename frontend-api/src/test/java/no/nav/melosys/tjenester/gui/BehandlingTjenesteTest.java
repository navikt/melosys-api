package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.saksopplysninger.SaksopplysningerTilDto;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.UTSENDT_ARBEIDSTAKER;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.UTSENDT_SELVSTENDIG;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.SOEKNAD;
import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.hamcrest.Matchers.equalTo;
import static org.jeasy.random.FieldPredicates.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BehandlingTjeneste.class})
class BehandlingTjenesteTest {

    @MockBean
    private BehandlingService behandlingService;
    @MockBean
    private SaksopplysningerTilDto saksopplysningerTilDto;
    @MockBean
    private SaksbehandlerService saksbehandlerService;
    @MockBean
    private BehandlingsresultatService behandlingsresultatService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private EasyRandom random;

    private static final long BEHANDLING_ID = 11L;
    private static final List<Long> PERIODE_IDER = Arrays.asList(2L, 3L, 5L);
    private static final String BASE_URL = "/api/behandlinger";
    private final Behandlingsresultat BEHANDLINGSRESULTAT = new Behandlingsresultat();
    private static final Set<Behandlingsstatus> MULIGE_STATUSER = Set.of(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING);
    private static final Set<Behandlingstema> MULIGE_BEHANDLINGSTEMA = Set.of(UTSENDT_ARBEIDSTAKER, UTSENDT_SELVSTENDIG);
    private static final Set<Behandlingstyper> MULIGE_TYPER = Set.of(NY_VURDERING);

    @BeforeEach
    void setUp() {

        random = new EasyRandom(new EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .objectPoolSize(100)
            .dateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
            .excludeField(named("tilleggsinformasjonDetaljer").and(ofType(TilleggsinformasjonDetaljer.class)).and(inClass(Tilleggsinformasjon.class)))
            .excludeField(named("sed").and(ofType(SedDokument.class)))
            .stringLengthRange(2, 10)
            .randomize(GeografiskAdresse.class, () -> random.nextObject(SemistrukturertAdresse.class))
            .randomize(MidlertidigPostadresse.class, () -> Math.random() > 0.5 ? random.nextObject(MidlertidigPostadresseNorge.class) : random.nextObject(MidlertidigPostadresseUtland.class))
            .randomize(named("fnr").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("fnrAnnenForelder").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9))
        );
    }

    @Test
    void endreBehandling() throws Exception {
        final var sakstype = Sakstyper.EU_EOS;
        final var behandlingstype = SOEKNAD;
        final var behandlingstema = Behandlingstema.ARBEID_I_UTLANDET;
        final var behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING;
        final var behandlingsfrist = LocalDate.now();
        final var sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG;

        var endreBehandlingDto = new EndreBehandlingDto(sakstype, behandlingstype, behandlingstema, behandlingsstatus, behandlingsfrist, sakstema);
        mockMvc.perform(post(BASE_URL + "/{behandlingID}/endre", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(endreBehandlingDto)))
            .andExpect(status().isNoContent());

        verify(behandlingService).endreBehandling(BEHANDLING_ID, sakstype, behandlingstype, behandlingstema, behandlingsstatus, behandlingsfrist, sakstema);
    }

    @Test
    void endreStatus() throws Exception {
        var endreBehandlingsstatusDto = new EndreBehandlingsstatusDto("UNDER_BEHANDLING");
        mockMvc.perform(post(BASE_URL + "/{behandlingID}/status", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(endreBehandlingsstatusDto)))
            .andExpect(status().isNoContent());
    }

    @Test
    void knyttMedlemsperioder() throws Exception {
        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = PERIODE_IDER;

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/tidligere-medlemsperioder", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tidligereMedlemsperioderDto)))
            .andExpect(status().isOk());
        verify(behandlingService).knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDER);
    }

    @Test
    void hentMedlemsperioder() throws Exception {
        when(behandlingService.hentMedlemsperioder(BEHANDLING_ID)).thenReturn(PERIODE_IDER);
        var dto = new TidligereMedlemsperioderDto();
        dto.periodeIder = PERIODE_IDER;

        mockMvc.perform(get(BASE_URL + "/{behandlingID}/tidligere-medlemsperioder", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(dto, TidligereMedlemsperioderDto.class));

        verify(behandlingService).hentMedlemsperioder(BEHANDLING_ID);
    }

    @Test
    void hentBehandling() throws Exception {
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(opprettTomBehandlingMedId());
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void endreBehandlingstema() throws Exception {
        EndreBehandlingstemaDto dto = new EndreBehandlingstemaDto("ARBEID_FLERE_LAND");

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/endreBehandlingstema", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isNoContent());
    }

    @Test
    void endreBehandlingsfrist() throws Exception {
        LocalDate frist = LocalDate.now().plusWeeks(1);
        EndreBehandlingsfristDto endreBehandlingsfristDto = new EndreBehandlingsfristDto(frist);

        mockMvc.perform(post(BASE_URL + "/{behandlingID}/behandlingsfrist", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(endreBehandlingsfristDto)))
            .andExpect(status().isNoContent());

        verify(behandlingService).endreBehandlingsfrist(BEHANDLING_ID, frist);
    }

    @Test
    void avsluttNyVurderingMedFerdigbehandlet() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{behandlingID}/sett-til-ferdigbehandlet", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());
    }

    @Test
    void hentMuligeStatuser() throws Exception {
        when(behandlingService.hentMuligeStatuser(BEHANDLING_ID)).thenReturn(MULIGE_STATUSER);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}/mulige-statuser", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", equalTo(MULIGE_STATUSER.size())));
    }

    @Test
    void hentMuligeBehandlingstema() throws Exception {
        when(behandlingService.hentMuligeBehandlingstema(BEHANDLING_ID)).thenReturn(MULIGE_BEHANDLINGSTEMA);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}/mulige-behandlingstema", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", equalTo(MULIGE_BEHANDLINGSTEMA.size())));
    }

    @Test
    void hentMuligeTyper() throws Exception {
        when(behandlingService.hentMuligeTyper(BEHANDLING_ID)).thenReturn(MULIGE_TYPER);

        mockMvc.perform(get(BASE_URL + "/{behandlingID}/mulige-typer", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()", equalTo(MULIGE_TYPER.size())));
    }

    private Behandling opprettTomBehandlingMedId() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        return behandling;
    }
}
