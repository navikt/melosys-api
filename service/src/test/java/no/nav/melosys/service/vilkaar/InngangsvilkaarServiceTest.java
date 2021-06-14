package no.nav.melosys.service.vilkaar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.domain.dokument.person.StatsborgerskapPeriode;
import no.nav.melosys.domain.inngangsvilkar.Feilmelding;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;
import no.nav.melosys.domain.inngangsvilkar.Kategori;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.inngangsvilkar.InngangsvilkaarConsumerImpl;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.dokument.felles.Land.*;
import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InngangsvilkaarServiceTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private InngangsvilkaarConsumerImpl inngangsvilkaarConsumer;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private SaksopplysningerService saksopplysningerService;
    @Mock
    private VilkaarsresultatService vilkaarsresultatService;
    private final FakeUnleash unleash = new FakeUnleash();

    private InngangsvilkaarService inngangsvilkaarService;

    @BeforeEach
    void setUp() {
        inngangsvilkaarService = new InngangsvilkaarService(behandlingService, inngangsvilkaarConsumer,
            persondataFasade, saksopplysningerService, unleash, vilkaarsresultatService);
    }

    @Test
    void vurderOgLagreInngangsvilkår() {
        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().plusYears(1), LocalDate.MAX);
        PersonDokument personDokument = new PersonDokument();
        personDokument.setStatsborgerskap(Land.av(FINLAND));
        when(saksopplysningerService.hentPersonOpplysninger(anyLong())).thenReturn(personDokument);
        InngangsvilkarResponse res = new InngangsvilkarResponse();
        res.setFeilmeldinger(Collections.emptyList());
        res.setKvalifisererForEf883_2004(Boolean.TRUE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), any())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, periode);

        verify(inngangsvilkaarConsumer).vurderInngangsvilkår(Set.of(personDokument.getStatsborgerskap()),
            Set.copyOf(tilIso3(landkoder)), periode);
        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR, true, Collections.emptySet());
    }

    @Test
    void vurderOgLagreInngangsvilkår_statsborgerskapFraPDL() {
        unleash.enable("melosys.pdl.statsborgerskap");
        final List<String> søknadsland = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().plusYears(1), LocalDate.MAX);
        final String ident = "aktørID";
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(lagBehandling(ident));
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
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), any())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, søknadsland, periode);

        verify(inngangsvilkaarConsumer).vurderInngangsvilkår(Set.of(Land.av(FINLAND), Land.av(SVERIGE)),
            Set.copyOf(tilIso3(søknadsland)), periode);
        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR, true,
            Collections.emptySet());
    }

    private Behandling lagBehandling(String aktørID) {
        Fagsak fagsak = new Fagsak();
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId(aktørID);
        fagsak.setAktører(Set.of(aktoer));
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        return behandling;
    }

    @Test
    void vurderOgLagreInngangsvilkår_manglerStatsborgerskap_girBegrunnelse() {
        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1));
        final var personhistorikkDokument = new PersonhistorikkDokument();
        personhistorikkDokument.statsborgerskapListe = Collections.emptyList();
        when(saksopplysningerService.hentPersonhistorikk(anyLong())).thenReturn(personhistorikkDokument);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, periode);

        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR,
            false, Set.of(Inngangsvilkaar.MANGLER_STATSBORGERSKAP));
    }

    @Test
    void vurderOgLagreInngangsvilkår_tomDatoErNull_tomDatoSettesTilEttÅrEtterFomDato() {
        ArgumentCaptor<no.nav.melosys.domain.behandlingsgrunnlag.data.Periode> søknadsperiodeCaptor = ArgumentCaptor.forClass(no.nav.melosys.domain.behandlingsgrunnlag.data.Periode.class);

        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().plusYears(1), null);
        PersonDokument personDokument = new PersonDokument();
        personDokument.setStatsborgerskap(Land.av(FINLAND));
        when(saksopplysningerService.hentPersonOpplysninger(anyLong())).thenReturn(personDokument);
        InngangsvilkarResponse res = new InngangsvilkarResponse();
        res.setFeilmeldinger(Collections.emptyList());
        res.setKvalifisererForEf883_2004(Boolean.TRUE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), søknadsperiodeCaptor.capture())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, periode);

        no.nav.melosys.domain.behandlingsgrunnlag.data.Periode søknadsperiode = søknadsperiodeCaptor.getValue();
        assertThat(søknadsperiode.getTom()).isEqualTo(LocalDate.now().plusYears(2));
    }

    @Test
    void vurderOgLagreInngangsvilkår_feil_girBegrunnelse() {
        final List<String> landkoder = List.of("FR", "DK", "NO");
        final var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().plusYears(1), LocalDate.MAX);
        PersonDokument personDokument = new PersonDokument();
        personDokument.setStatsborgerskap(Land.av(FINLAND));
        when(saksopplysningerService.hentPersonOpplysninger(anyLong())).thenReturn(personDokument);
        InngangsvilkarResponse res = new InngangsvilkarResponse();
        var feilmelding = new Feilmelding();
        feilmelding.setKategori(Kategori.TEKNISK_FEIL);
        feilmelding.setMelding("FEIL!!!");
        res.setFeilmeldinger(Collections.singletonList(feilmelding));
        res.setKvalifisererForEf883_2004(Boolean.FALSE);
        when(inngangsvilkaarConsumer.vurderInngangsvilkår(any(), anySet(), any())).thenReturn(res);

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, periode);

        verify(inngangsvilkaarConsumer).vurderInngangsvilkår(Set.of(personDokument.getStatsborgerskap()),
            Set.copyOf(tilIso3(landkoder)), periode);
        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(1L, Vilkaar.FO_883_2004_INNGANGSVILKAAR,
            false, Set.of(Inngangsvilkaar.TEKNISK_FEIL));
    }

    @Test
    void avgjørStatsborgerskapPåStartDato_tomListe_girNull() {
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(new ArrayList<>(), null);
        assertThat(stastborgerskap).isNull();
    }

    @Test
    void avgjørStatsborgerskapPåStartDato_ingenGyldige_girNull() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(UKJENT);
        p2.periode = new Periode(LocalDate.of(2018, 4, 1), LocalDate.of(2018, 5, 2));
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2019, 2, 1));
        assertThat(stastborgerskap).isNull();
    }

    @Test
    void avgjørStatsborgerskapPåStartDato_flerePerioder_girPeriodenSomInkludererStartdato() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(UKJENT);
        p2.periode = new Periode(LocalDate.of(2018, 4, 1), null);
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
        assertThat(stastborgerskap).isEqualTo(av(BELGIA));
    }

    @Test
    void avgjørStatsborgerskapPåStartDato_flerePerioder_filtererSkd() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        p1.endretAv = "NAV";
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(UKJENT);
        p2.periode = new Periode(LocalDate.of(2017, 4, 1), null);
        p2.endretAv = "SKD";
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
        assertThat(stastborgerskap).isEqualTo(av(BELGIA));
    }

    @Test
    void avgjørStatsborgerskapPåStartDato_flereGyldige_filtrererUkjent() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        p1.endretAv = "NAV";
        p1.endringstidspunkt = LocalDateTime.now().minusYears(3);
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(UKJENT);
        p2.periode = new Periode(LocalDate.of(2017, 4, 1), null);
        p2.endretAv = "NAV";
        p2.endringstidspunkt = LocalDateTime.now().minusYears(2);
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
        assertThat(stastborgerskap).isEqualTo(av(BELGIA));
    }

    @Test
    void avgjørStatsborgerskapPåStartDato_flereGyldige_girSistEndret() {
        List<StatsborgerskapPeriode> statsborgerskapPerioder = new ArrayList<>();
        StatsborgerskapPeriode p1 = new StatsborgerskapPeriode();
        p1.statsborgerskap = av(BELGIA);
        p1.periode = new Periode(LocalDate.of(2007, 3, 27), LocalDate.of(2018, 3, 27));
        p1.endretAv = "NAV";
        p1.endringstidspunkt = LocalDateTime.now().minusYears(3);
        statsborgerskapPerioder.add(p1);
        StatsborgerskapPeriode p2 = new StatsborgerskapPeriode();
        p2.statsborgerskap = av(SVERIGE);
        p2.periode = new Periode(LocalDate.of(2017, 4, 1), null);
        p2.endretAv = "NAV";
        p2.endringstidspunkt = LocalDateTime.now().minusYears(2);
        statsborgerskapPerioder.add(p2);
        Land stastborgerskap = inngangsvilkaarService.avgjørStatsborgerskapPåStartDato(statsborgerskapPerioder, LocalDate.of(2018, 2, 1));
        assertThat(stastborgerskap).isEqualTo(av(SVERIGE));
    }

    @Test
    void avgjørGyldigeStatsborgerskapFraPdl() {
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
        var periode = new no.nav.melosys.domain.behandlingsgrunnlag.data.Periode(LocalDate.now().plusMonths(1), null);

        final Set<Land> statsborgerskap = inngangsvilkaarService.avgjørGyldigeStatsborgerskapFraPdlForPerioden(statsborgerskapFraPdl,
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
    void overstyrInngangsvilkårTilOppfylt_inngangsvilkårFunnet_oppfyllerVilkår() {
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        when(vilkaarsresultatService.finnVilkaarsresultat(anyLong(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR))).thenReturn(Optional.of(vilkaarsresultat));

        inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L);

        verify(vilkaarsresultatService).oppdaterVilkaarsresultat(eq(1L), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR), eq(true), anySet());
    }

    @Test
    void overstyrInngangsvilkårTilOppfylt_inngangsvilkårFunnet_beholderGamleBegrunnelserOgLeggerTilOverstyringsbegrunnelse() {
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
}
