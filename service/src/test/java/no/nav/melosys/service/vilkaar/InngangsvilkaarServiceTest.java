package no.nav.melosys.service.vilkaar;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.inngangsvilkar.Feilmelding;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;
import no.nav.melosys.domain.inngangsvilkar.Kategori;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData;
import no.nav.melosys.domain.mottatteopplysninger.data.Periode;
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.inngangsvilkar.InngangsvilkaarConsumerImpl;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.dokument.felles.Land.FINLAND;
import static no.nav.melosys.domain.dokument.felles.Land.SVERIGE;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE;
import static no.nav.melosys.domain.util.IsoLandkodeKonverterer.tilIso3;
import static no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InngangsvilkaarServiceTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private InngangsvilkaarConsumerImpl inngangsvilkaarConsumer;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;
    @Mock
    private VilkaarsresultatService vilkaarsresultatService;

    private InngangsvilkaarService inngangsvilkaarService;

    @BeforeEach
    void setUp() {
        inngangsvilkaarService = new InngangsvilkaarService(behandlingService, inngangsvilkaarConsumer,
            persondataFasade, vilkaarsresultatService, saksbehandlingRegler);
    }

    @Test
    void vurderOgLagreInngangsvilkår_medFlereGyldigeStatsborgerskap_oppdaterVilkårsresultat() {
        final List<String> søknadsland = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.mottatteopplysninger.data.Periode(LocalDate.now().plusYears(1), LocalDate.MAX);
        final String ident = "aktørID";
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        final Set<Statsborgerskap> statsborgerskap = Set.of(
            new no.nav.melosys.domain.person.Statsborgerskap("FIN", null, LocalDate.parse("1989-11-18"), null, "FREG",
                "Dolly", false),
            new no.nav.melosys.domain.person.Statsborgerskap("SWE", LocalDate.parse("2009-11-18"), null, null, "PDL",
                "Dolly", false)
        );
        when(persondataFasade.hentStatsborgerskap(ident)).thenReturn(statsborgerskap);

        InngangsvilkarResponse res = new InngangsvilkarResponse();
        res.setFeilmeldinger(Collections.emptyList());
        res.setKvalifisererForEf883_2004(Boolean.TRUE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), anyBoolean(), any())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, søknadsland, false, periode);

        verify(inngangsvilkaarConsumer).vurderInngangsvilkår(Set.of(Land.av(FINLAND), Land.av(SVERIGE)),
            Set.copyOf(tilIso3(søknadsland)), false, periode);
        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR, true,
            Collections.emptySet());
    }

    @Test
    void vurderOgLagreInngangsvilkår_manglerStatsborgerskap_girBegrunnelse() {
        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.mottatteopplysninger.data.Periode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1));
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(persondataFasade.hentStatsborgerskap(any())).thenReturn(Collections.emptySet());

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, false, periode);

        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR,
            false, Set.of(Inngangsvilkaar.MANGLER_STATSBORGERSKAP));
    }

    @Test
    void vurderOgLagreInngangsvilkår_tomDatoErNull_tomDatoSettesTilEttÅrEtterFomDato() {
        ArgumentCaptor<no.nav.melosys.domain.mottatteopplysninger.data.Periode> søknadsperiodeCaptor = ArgumentCaptor.forClass(no.nav.melosys.domain.mottatteopplysninger.data.Periode.class);

        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.mottatteopplysninger.data.Periode(LocalDate.now().plusYears(1), null);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        final Set<Statsborgerskap> statsborgerskap = Set.of(
            new no.nav.melosys.domain.person.Statsborgerskap("FIN", null, LocalDate.parse("1989-11-18"), null, "FREG",
                "Dolly", false)
        );
        when(persondataFasade.hentStatsborgerskap(any())).thenReturn(statsborgerskap);
        InngangsvilkarResponse res = new InngangsvilkarResponse();
        res.setFeilmeldinger(Collections.emptyList());
        res.setKvalifisererForEf883_2004(Boolean.TRUE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), anyBoolean(), søknadsperiodeCaptor.capture())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, false, periode);

        no.nav.melosys.domain.mottatteopplysninger.data.Periode søknadsperiode = søknadsperiodeCaptor.getValue();
        assertThat(søknadsperiode.getTom()).isEqualTo(LocalDate.now().plusYears(2));
    }

    @Test
    void vurderOgLagreInngangsvilkår_flereLandUkjentHvilke() {
        final var periode = new no.nav.melosys.domain.mottatteopplysninger.data.Periode(LocalDate.now().plusYears(1), LocalDate.MAX);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        final Set<Statsborgerskap> statsborgerskap = Set.of(
            new no.nav.melosys.domain.person.Statsborgerskap("FIN", null, LocalDate.parse("1989-11-18"), null, "FREG",
                "Dolly", false)
        );
        when(persondataFasade.hentStatsborgerskap(any())).thenReturn(statsborgerskap);
        InngangsvilkarResponse res = new InngangsvilkarResponse();
        res.setFeilmeldinger(Collections.emptyList());
        res.setKvalifisererForEf883_2004(Boolean.TRUE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), anyBoolean(), any())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, Collections.emptyList(), true, periode);

        verify(inngangsvilkaarConsumer).vurderInngangsvilkår(Collections.singleton(Land.av(FINLAND)),
            Collections.emptySet(), true, periode);
        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR, true, Collections.emptySet());
    }

    @Test
    void vurderOgLagreInngangsvilkår_feil_girBegrunnelse() {
        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.mottatteopplysninger.data.Periode(LocalDate.now().plusYears(1), LocalDate.MAX);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        final Set<Statsborgerskap> statsborgerskap = Set.of(
            new no.nav.melosys.domain.person.Statsborgerskap("FIN", null, LocalDate.parse("1989-11-18"), null, "FREG",
                "Dolly", false)
        );
        when(persondataFasade.hentStatsborgerskap(any())).thenReturn(statsborgerskap);
        InngangsvilkarResponse res = new InngangsvilkarResponse();
        var feilmelding = new Feilmelding();
        feilmelding.setKategori(Kategori.TEKNISK_FEIL);
        feilmelding.setMelding("FEIL!!!");
        res.setFeilmeldinger(Collections.singletonList(feilmelding));
        res.setKvalifisererForEf883_2004(Boolean.FALSE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), anyBoolean(), any())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, false, periode);

        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR,
            false, Set.of(Inngangsvilkaar.TEKNISK_FEIL));
    }

    @Test
    void avgjørGyldigeStatsborgerskapForPerioden() {
        var statsborgerskapFraPdl = Set.of(
            new no.nav.melosys.domain.person.Statsborgerskap(
                "AAA", null, LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"),
                "FREG", "Holly", false),
            new no.nav.melosys.domain.person.Statsborgerskap(
                "BBB", null, LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"),
                "PDL", "Dolly", false),
            new no.nav.melosys.domain.person.Statsborgerskap(
                "CCC", null, LocalDate.parse("2020-11-18"), null,
                "PDL", "Molly", false),
            new no.nav.melosys.domain.person.Statsborgerskap(
                "DDD", LocalDate.parse("2021-05-08"), LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"),
                "PDL", "Molly", false),
            new no.nav.melosys.domain.person.Statsborgerskap(
                "EEE", null, null, null,
                "FREG", "Nully", false)
        );
        var periode = new no.nav.melosys.domain.mottatteopplysninger.data.Periode(LocalDate.parse("2020-11-18"), null);

        final Set<Land> statsborgerskap = inngangsvilkaarService.avgjørGyldigeStatsborgerskapForPerioden(statsborgerskapFraPdl,
            periode);

        assertThat(statsborgerskap).containsExactlyInAnyOrder(Land.av("CCC"), Land.av("DDD"), Land.av("EEE"));
    }

    @Test
    void overstyrInngangsvilkårTilOppfylt_ingenInngangsvilkårFunnet_kasterFunksjonellException() {
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR))).thenReturn(Optional.empty());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L))
            .withMessage("Inngangsvilkår er ikke vurdert for behandling 1");
    }

    @Test
    void overstyrInngangsvilkårTilOppfylt_manglerLandOgPeriode_kasterFunksjonellException() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagBehandling());
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR))).thenReturn(Optional.of(new Vilkaarsresultat()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L))
            .withMessage("Mangler land eller periode for behandling 1");
    }

    @Test
    void overstyrInngangsvilkårTilOppfylt_inngangsvilkårFunnet_oppfyllerVilkår() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagBehandlingMedPeriodeOgLand());
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR))).thenReturn(Optional.of(vilkaarsresultat));


        inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L);


        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(eq(1L), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR), eq(true), anySet());
    }

    @Test
    void overstyrInngangsvilkårTilOppfylt_inngangsvilkårFunnet_beholderGamleBegrunnelserOgLeggerTilOverstyringsbegrunnelse() {
        when(behandlingService.hentBehandling(1L)).thenReturn(lagBehandlingMedPeriodeOgLand());
        VilkaarBegrunnelse vilkaarBegrunnelse = new VilkaarBegrunnelse();
        vilkaarBegrunnelse.setKode(Inngangsvilkaar.MANGLER_STATSBORGERSKAP.getKode());
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setBegrunnelser(Set.of(vilkaarBegrunnelse));
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR))).thenReturn(Optional.of(vilkaarsresultat));


        inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L);


        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(eq(1L), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR), anyBoolean(), eq(Set.of(
            Inngangsvilkaar.OVERSTYRT_AV_SAKSBEHANDLER, Inngangsvilkaar.MANGLER_STATSBORGERSKAP
        )));
    }

    @Test
    void skalVurdereInngangsvilkår_altStemmer_returnererTrue() {
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        var behandling = lagBehandlingMedPeriodeOgLand();
        behandling.setFagsak(fagsak);


        assertThat(inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)).isTrue();
    }

    @Test
    void skalVurdereInngangsvilkår_sakstypeIkkeEøs_returnererFalse() {
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.FTRL);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);


        assertThat(inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)).isFalse();
        verifyNoInteractions(saksbehandlingRegler);
    }

    @Test
    void skalVurdereInngangsvilkår_harIngenFlyt_returnererFalse() {
        when(saksbehandlingRegler.harIngenFlyt(any())).thenReturn(true);
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);


        assertThat(inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)).isFalse();
        verify(saksbehandlingRegler).harIngenFlyt(any());
        verify(saksbehandlingRegler, never()).harRegistreringUnntakFraMedlemskapFlyt(any());
        verify(saksbehandlingRegler, never()).harIkkeYrkesaktivFlyt(any());
    }

    @Test
    void skalVurdereInngangsvilkår_harUnntaktsregistreringFlyt_returnererFalse() {
        when(saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any())).thenReturn(true);
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);


        assertThat(inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)).isFalse();
        verify(saksbehandlingRegler).harIngenFlyt(any());
        verify(saksbehandlingRegler).harRegistreringUnntakFraMedlemskapFlyt(any());
        verify(saksbehandlingRegler, never()).harIkkeYrkesaktivFlyt(any());
    }

    @Test
    void skalVurdereInngangsvilkår_harIkkeYrkeskaktivFlyt_returnererFalse() {
        when(saksbehandlingRegler.harIkkeYrkesaktivFlyt(any())).thenReturn(true);
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);


        assertThat(inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)).isFalse();
        verify(saksbehandlingRegler).harIngenFlyt(any());
        verify(saksbehandlingRegler).harRegistreringUnntakFraMedlemskapFlyt(any());
        verify(saksbehandlingRegler).harIkkeYrkesaktivFlyt(any());
    }

    @Test
    void skalVurdereInngangsvilkår_erSed_returnererFalse() {
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        var behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setTema(REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE);


        assertThat(inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)).isFalse();
        verify(saksbehandlingRegler).harIngenFlyt(any());
        verify(saksbehandlingRegler).harRegistreringUnntakFraMedlemskapFlyt(any());
        verify(saksbehandlingRegler).harIkkeYrkesaktivFlyt(any());
    }

    @Test
    void skalVurdereInngangsvilkår_harIkkePeriode_returnererFalse() {
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        var behandling = lagBehandlingMedPeriodeOgLand();
        behandling.setFagsak(fagsak);
        behandling.getMottatteOpplysninger().getMottatteOpplysningerData().periode = new Periode(null, null);


        assertThat(inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)).isFalse();
        verify(saksbehandlingRegler).harIngenFlyt(any());
        verify(saksbehandlingRegler).harRegistreringUnntakFraMedlemskapFlyt(any());
        verify(saksbehandlingRegler).harIkkeYrkesaktivFlyt(any());

    }

    @Test
    void skalVurdereInngangsvilkår_harIkkeLand_returnererFalse() {
        var fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        var behandling = lagBehandlingMedPeriodeOgLand();
        behandling.setFagsak(fagsak);
        behandling.getMottatteOpplysninger().getMottatteOpplysningerData().soeknadsland.setLandkoder(Collections.emptyList());


        assertThat(inngangsvilkaarService.skalVurdereInngangsvilkår(behandling)).isFalse();
        verify(saksbehandlingRegler).harIngenFlyt(any());
        verify(saksbehandlingRegler).harRegistreringUnntakFraMedlemskapFlyt(any());
        verify(saksbehandlingRegler).harIkkeYrkesaktivFlyt(any());
    }

    private Behandling lagBehandlingMedPeriodeOgLand() {
        var mottatteOpplysningerData = new MottatteOpplysningerData();
        mottatteOpplysningerData.periode = new Periode(LocalDate.now(), null);
        mottatteOpplysningerData.soeknadsland = new Soeknadsland(List.of(Landkoder.BE.getKode()), false);
        var behandling = new Behandling();
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        behandling.getMottatteOpplysninger().setMottatteOpplysningerData(mottatteOpplysningerData);
        return behandling;
    }
}
