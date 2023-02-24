package no.nav.melosys.tjenester.gui;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland;
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie;
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie;
import no.nav.melosys.domain.person.familie.OmfattetFamilie;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleResultat;
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleService;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleInfoDto;
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleResultatDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie.Relasjonsrolle;
import static no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie.tilMedfolgendeFamilie;
import static no.nav.melosys.tjenester.gui.util.ResponseBodyMatchers.responseBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {TrygdeavtaleTjeneste.class})
class TrygdeavtaleTjenesteTest {
    private final static String ORGNR_1 = "11111111111";
    private final static String UUID_BARN_1 = UUID.randomUUID().toString();
    private final static String UUID_BARN_2 = UUID.randomUUID().toString();
    private final static String UUID_EKTEFELLE = UUID.randomUUID().toString();
    private final static String BEGRUNNELSE_BARN = "begrunnelse barn";
    private final static String BEGRUNNELSE_SAMBOER = "begrunnelse samboer";
    private static final String EKTEFELLE_FNR = "01108049800";
    private static final String BARN1_FNR = "01100099728";
    private static final String BARN2_FNR = "02109049878";
    private static final String BARN_NAVN_1 = "Doffen Duck";
    private static final String BARN_NAVN_2 = "Dole Duck";
    private static final String EKTEFELLE_NAVN = "Dolly Duck";

    @MockBean
    private TrygdeavtaleService trygdeavtaleService;
    @MockBean
    private BehandlingService behandlingService;
    @MockBean
    private BehandlingsresultatService behandlingsresultatService;
    @MockBean
    private Aksesskontroll aksesskontroll;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<TrygdeavtaleResultat> trygdeavtaleResultatArgumentCaptor;

    private static Behandling behandling;
    private static Behandlingsresultat behandlingsresultat;

    private static final String BASE_URL = "/api/trygdeavtale";

    @BeforeEach
    void setup() {
        behandling = lagBehandling();
        behandlingsresultat = lagBehandlingsresultat();
    }

    @Test
    void overførResultat_medTrygdeavtaleResultatDto_mappesKorrekt() throws Exception {
        var trygdeavtaleResultatDto = lagTrygdeavtaleResultatDto();

        mockMvc.perform(post(BASE_URL + "/resultat/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trygdeavtaleResultatDto))
            )
            .andExpect(status().isNoContent());


        verify(trygdeavtaleService).overførResultat(eq(1L), trygdeavtaleResultatArgumentCaptor.capture());
        var trygdeavtaleResultat = trygdeavtaleResultatArgumentCaptor.getValue();

        assertThat(trygdeavtaleResultat)
            .isNotNull()
            .extracting(
                TrygdeavtaleResultat::virksomhet,
                TrygdeavtaleResultat::bestemmelse,
                TrygdeavtaleResultat::lovvalgsperiodeFom,
                TrygdeavtaleResultat::lovvalgsperiodeTom
            )
            .containsExactlyInAnyOrder(
                trygdeavtaleResultatDto.virksomhet(),
                trygdeavtaleResultatDto.bestemmelse(),
                trygdeavtaleResultatDto.lovvalgsperiodeFom(),
                trygdeavtaleResultatDto.lovvalgsperiodeTom()
            );
        assertThat(trygdeavtaleResultat.familie().getFamilieIkkeOmfattetAvNorskTrygd())
            .hasSize(2)
            .flatExtracting(
                IkkeOmfattetFamilie::getUuid,
                IkkeOmfattetFamilie::getBegrunnelse,
                IkkeOmfattetFamilie::getBegrunnelseFritekst)
            .containsExactlyInAnyOrder(
                UUID_BARN_1, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(), BEGRUNNELSE_BARN,
                UUID_EKTEFELLE, Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(), BEGRUNNELSE_SAMBOER
            );
        assertThat(trygdeavtaleResultat.familie().getFamilieOmfattetAvNorskTrygd())
            .hasSize(1)
            .flatExtracting(OmfattetFamilie::getUuid)
            .containsExactly(UUID_BARN_2);
    }

    @Test
    void hentTrygdeavtaleInfo_utenVirksomhetOgBarnEktefelle_returnererKorrekt() throws Exception {
        when(behandlingService.hentBehandlingMedSaksopplysninger(1L)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);

        MvcResult mvcResult = mockMvc.perform(get(BASE_URL + "/mottatteopplysninger/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .requestAttr("virksomheter", false)
                .requestAttr("barnEktefeller", false)
            )
            .andExpect(status().isOk())
            .andReturn();


        String body = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var response = objectMapper.readValue(body, TrygdeavtaleInfoDto.class);

        verify(trygdeavtaleService, never()).hentVirksomheter(any());
        verify(trygdeavtaleService, never()).hentFamiliemedlemmer(any());

        assertThat(response).isNotNull();
        assertThat(response.aktoerId()).isEqualTo(behandling.getFagsak().hentBrukersAktørID());
        assertThat(response.behandlingstema()).isEqualTo(behandling.getTema().getKode());
        assertThat(response.behandlingstype()).isEqualTo(behandling.getType().getKode());
        assertThat(response.redigerbart()).isFalse();
        var mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
        assertThat(response.periodeFom()).isEqualTo(mottatteOpplysningerData.periode.getFom());
        assertThat(response.periodeTom()).isEqualTo(mottatteOpplysningerData.periode.getTom());
        assertThat(response.soeknadsland()).isEqualTo(Trygdeavtale_myndighetsland.GB);
        assertThat(response.innledningFritekst()).isEqualTo(behandlingsresultat.getInnledningFritekst());
        assertThat(response.begrunnelseFritekst()).isEqualTo(behandlingsresultat.getBegrunnelseFritekst());
        assertThat(response.nyVurderingBakgrunn()).isNull();
    }

    @Test
    void hentTrygdeavtaleInfo_medVirksomhetOgBarnEktefelle_returnererKorrekt() throws Exception {
        when(behandlingService.hentBehandlingMedSaksopplysninger(1L)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);

        MvcResult mvcResult = mockMvc.perform(get(BASE_URL + "/mottatteopplysninger/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .param("virksomheter", "true")
                .param("barnEktefeller", "true")
            )
            .andExpect(status().isOk())
            .andReturn();


        String body = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var response = objectMapper.readValue(body, TrygdeavtaleInfoDto.class);

        verify(trygdeavtaleService).hentVirksomheter(any());
        verify(trygdeavtaleService).hentFamiliemedlemmer(any());

        assertThat(response).isNotNull();
        assertThat(response.aktoerId()).isEqualTo(behandling.getFagsak().hentBrukersAktørID());
        assertThat(response.behandlingstema()).isEqualTo(behandling.getTema().getKode());
        assertThat(response.behandlingstype()).isEqualTo(behandling.getType().getKode());
        assertThat(response.redigerbart()).isFalse();
        var mottatteOpplysningerData = behandling.getMottatteOpplysninger().getMottatteOpplysningerData();
        assertThat(response.periodeFom()).isEqualTo(mottatteOpplysningerData.periode.getFom());
        assertThat(response.periodeTom()).isEqualTo(mottatteOpplysningerData.periode.getTom());
        assertThat(response.soeknadsland()).isEqualTo(Trygdeavtale_myndighetsland.GB);
        assertThat(response.innledningFritekst()).isEqualTo(behandlingsresultat.getInnledningFritekst());
        assertThat(response.begrunnelseFritekst()).isEqualTo(behandlingsresultat.getBegrunnelseFritekst());
        assertThat(response.nyVurderingBakgrunn()).isNull();
    }

    @Test
    void hentTrygdeavtaleInfo_somNyVurdering_returnererKorrekt() throws Exception {
        var vedtakMetadata = new VedtakMetadata();
        vedtakMetadata.setNyVurderingBakgrunn(Nyvurderingbakgrunner.NYE_OPPLYSNINGER.getKode());
        behandlingsresultat.setVedtakMetadata(vedtakMetadata);
        behandling.setType(Behandlingstyper.NY_VURDERING);

        when(behandlingService.hentBehandlingMedSaksopplysninger(1L)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);

        MvcResult mvcResult = mockMvc.perform(get(BASE_URL + "/mottatteopplysninger/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .requestAttr("virksomheter", true)
                .requestAttr("barnEktefeller", true)
            )
            .andExpect(status().isOk())
            .andReturn();


        String body = mvcResult.getResponse().getContentAsString();
        var response = objectMapper.readValue(body, TrygdeavtaleInfoDto.class);

        assertThat(response).isNotNull();
        assertThat(response.behandlingstype()).isEqualTo(Behandlingstyper.NY_VURDERING.getKode());
        assertThat(response.nyVurderingBakgrunn()).isEqualTo(vedtakMetadata.getNyVurderingBakgrunn());
    }

    @Test
    void hentTrygdeavtaleInfo_saksbehandlerHarTillatelseTilÅRedigere_returnererKorrekt() throws Exception {
        when(behandlingService.hentBehandlingMedSaksopplysninger(1L)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat);
        when(aksesskontroll.behandlingKanRedigeresAvSaksbehandler(eq(behandling), any())).thenReturn(true);

        MvcResult mvcResult = mockMvc.perform(get(BASE_URL + "/mottatteopplysninger/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .requestAttr("virksomheter", true)
                .requestAttr("barnEktefeller", true)
            )
            .andExpect(status().isOk())
            .andReturn();


        String body = mvcResult.getResponse().getContentAsString();
        var response = objectMapper.readValue(body, TrygdeavtaleInfoDto.class);

        assertThat(response).isNotNull();
        assertThat(response.redigerbart()).isTrue();
    }

    @Test
    void hentResultat_byggOppResultat_returnererKorrekt() throws Exception {
        Behandling behandling = lagBehandling();
        behandling.getMottatteOpplysninger().getMottatteOpplysningerData().personOpplysninger.medfolgendeFamilie =
            List.of(
                tilMedfolgendeFamilie(UUID_EKTEFELLE, EKTEFELLE_FNR, EKTEFELLE_NAVN, Relasjonsrolle.EKTEFELLE_SAMBOER),
                tilMedfolgendeFamilie(UUID_BARN_1, BARN1_FNR, BARN_NAVN_1, MedfolgendeFamilie.Relasjonsrolle.BARN),
                tilMedfolgendeFamilie(UUID_BARN_2, BARN2_FNR, BARN_NAVN_2, MedfolgendeFamilie.Relasjonsrolle.BARN)
            );

        when(behandlingService.hentBehandlingMedSaksopplysninger(1L)).thenReturn(behandling);
        when(trygdeavtaleService.hentResultat(1L)).thenReturn(lagTrygdeavtaleResultat());

        mockMvc.perform(get(BASE_URL + "/resultat/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(lagTrygdeavtaleResultatDto(), TrygdeavtaleResultatDto.class));
    }

    @Test
    void hentResultat_tomtResultat_returnererKorrekt() throws Exception {
        Behandling behandling = lagBehandling();
        behandling.getMottatteOpplysninger().getMottatteOpplysningerData().personOpplysninger.medfolgendeFamilie =
            List.of();

        TrygdeavtaleResultat tomtTrygdeavtaleResultat = new TrygdeavtaleResultat
            .Builder().familie(new AvklarteMedfolgendeFamilie(Set.of(), Set.of())).build();

        when(behandlingService.hentBehandlingMedSaksopplysninger(1L)).thenReturn(behandling);
        when(trygdeavtaleService.hentResultat(1L)).thenReturn(tomtTrygdeavtaleResultat);

        mockMvc.perform(get(BASE_URL + "/resultat/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(new TrygdeavtaleResultatDto.Builder().build(), TrygdeavtaleResultatDto.class));
    }

    private static MottatteOpplysninger lagMottatteOpplysninger() {
        var mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.soeknadsland.landkoder.add(Landkoder.GB.getKode());
        mottatteOpplysningerData.periode = new Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
        var mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerdata(mottatteOpplysningerData);
        return mottatteOpplysninger;
    }

    private static Behandling lagBehandling() {
        var bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("AktørId");
        var fagsak = new Fagsak();
        fagsak.getAktører().add(bruker);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setMottatteOpplysninger(lagMottatteOpplysninger());
        return behandling;
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        var behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setInnledningFritekst("innledningFritekst");
        behandlingsresultat.setBegrunnelseFritekst("begrunnelseFritekst");
        return behandlingsresultat;
    }

    private TrygdeavtaleResultatDto lagTrygdeavtaleResultatDto() {
        return new TrygdeavtaleResultatDto.Builder()
            .virksomhet(ORGNR_1)
            .bestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1.getKode())
            .lovvalgsperiodeFom(LocalDate.now())
            .lovvalgsperiodeTom(LocalDate.now().plusYears(1))
            .addBarn(
                UUID_BARN_1,
                false,
                Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(),
                BEGRUNNELSE_BARN)
            .addBarn(UUID_BARN_2, true, null, null)
            .ektefelle(
                UUID_EKTEFELLE,
                false,
                Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(),
                BEGRUNNELSE_SAMBOER)
            .build();
    }

    TrygdeavtaleResultat lagTrygdeavtaleResultat() {
        return new TrygdeavtaleResultat
            .Builder()
            .virksomhet(ORGNR_1)
            .lovvalgsperiodeFom(LocalDate.now())
            .lovvalgsperiodeTom(LocalDate.now().plusYears(1))
            .bestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1.getKode())
            .familie(lagAvklartMedfølgendeBarn()).build();
    }

    private AvklarteMedfolgendeFamilie lagAvklartMedfølgendeBarn() {
        var ektefelle = new IkkeOmfattetFamilie(UUID_EKTEFELLE, Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.getKode(), BEGRUNNELSE_SAMBOER);
        var barn1 = new IkkeOmfattetFamilie(UUID_BARN_1, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.getKode(), BEGRUNNELSE_BARN);
        barn1.setIdent(BARN1_FNR);
        var barn2 = new OmfattetFamilie(UUID_BARN_2);
        barn2.setIdent(BARN2_FNR);
        return new AvklarteMedfolgendeFamilie(
            Set.of(barn2), Set.of(ektefelle, barn1)
        );
    }
}
