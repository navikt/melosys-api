package no.nav.melosys.saksflyt.steg.medl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LagreAnmodningsperiodeIMedlTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private LagreAnmodningsperiodeIMedl lagreAnmodningsperiodeIMedl;

    private Prosessinstans prosessinstans;
    private Behandlingsresultat behandlingsresultat;
    private Behandling behandling;
    private final LocalDate NOW = LocalDate.now();

    @BeforeEach
    public void setUp() {
        lagreAnmodningsperiodeIMedl = new LagreAnmodningsperiodeIMedl(behandlingsresultatService, medlPeriodeService);

        prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .build();

        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(1L)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medTema(Behandlingstema.TRYGDETID)
            .build();

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(null, null, Land_iso2.CH,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, null, null, null, Trygdedekninger.FULL_DEKNING_EOSFO);

        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.ANMODNING_OM_UNNTAK)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .build();
    }

    @Test
    void utførNårBehandlingsresultatTypeErAnmodning_om_unntak() {
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        behandlingsresultat.setAnmodningsperioder(lagAnmodningsperioderMedDato(NOW, NOW.plusMonths(1)));

        lagreAnmodningsperiodeIMedl.utfør(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeUnderAvklaring(any(Anmodningsperiode.class), anyLong());
    }

    @Test
    void utførNårBehandlingsresultatHarIngenLovvalgPeriode() {
        behandlingsresultat.setAnmodningsperioder(new HashSet<>());

        assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> lagreAnmodningsperiodeIMedl.utfør(prosessinstans))
            .withMessageContaining("Ingen anmodningsperioder finnes");
    }

    @Test
    void utfør_ulogiskDato_lagrerIkke() {
        behandlingsresultat.setAnmodningsperioder(lagAnmodningsperioderMedDato(NOW, NOW.minusMonths(1)));

        lagreAnmodningsperiodeIMedl.utfør(prosessinstans);
        verify(medlPeriodeService, never()).opprettPeriodeUnderAvklaring(any(Anmodningsperiode.class), anyLong());
    }

    @Test
    void utfør_oppdaterAnmodningsperiode_ok() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Behandling forrigeBehandling = BehandlingTestFactory.builderWithDefaults()
            .medId(2L)
            .medFagsak(fagsak)
            .medType(Behandlingstyper.NY_VURDERING)
            .medRegistrertDato(Instant.now().minusSeconds(10))
            .build();

        Behandling førsteBehandling = BehandlingTestFactory.builderWithDefaults()
            .medId(3L)
            .medFagsak(fagsak)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medRegistrertDato(Instant.now().minusSeconds(20))
            .build();
        Anmodningsperiode førsteAnmodningsperiode = new Anmodningsperiode();
        førsteAnmodningsperiode.setMedlPeriodeID(44L);

        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        behandling.setRegistrertDato(Instant.now());

        fagsak.leggTilBehandling(behandling);
        fagsak.leggTilBehandling(forrigeBehandling);
        fagsak.leggTilBehandling(førsteBehandling);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(NOW, NOW.plusMonths(1), null, null, null, null, null, null);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));

        when(behandlingsresultatService.hentBehandlingsresultat(forrigeBehandling.getId())).thenReturn(lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT, null));
        when(behandlingsresultatService.hentBehandlingsresultat(førsteBehandling.getId())).thenReturn(lagBehandlingsresultat(Behandlingsresultattyper.ANMODNING_OM_UNNTAK, førsteAnmodningsperiode));


        lagreAnmodningsperiodeIMedl.utfør(prosessinstans);


        verify(medlPeriodeService).oppdaterPeriodeUnderAvklaring(anmodningsperiode, behandling.getId());
    }

    private Set<Anmodningsperiode> lagAnmodningsperioderMedDato(LocalDate fom, LocalDate tom) {
        return Set.of(new Anmodningsperiode(fom, tom, null, null, null, null, null, null));
    }

    public Behandlingsresultat lagBehandlingsresultat(Behandlingsresultattyper behandlingsresultattyper, Anmodningsperiode anmodningsperiode) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(behandlingsresultattyper);
        if (anmodningsperiode != null) {
            behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        }

        return behandlingsresultat;
    }

}
