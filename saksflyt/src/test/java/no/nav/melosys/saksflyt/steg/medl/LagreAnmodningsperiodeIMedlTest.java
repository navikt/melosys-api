package no.nav.melosys.saksflyt.steg.medl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

        prosessinstans = new Prosessinstans();

        behandling = BehandlingTestBuilder.builderWithDefaults().build();
        behandling.setId(1L);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(null, null, Land_iso2.CH,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, null, null, null, Trygdedekninger.FULL_DEKNING_EOSFO);

        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        prosessinstans.setBehandling(behandling);
        prosessinstans.getBehandling().setType(Behandlingstyper.FØRSTEGANG);
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
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
    @Disabled("Fin ut av Wanted but not invoked: medlPeriodeService.oppdaterPeriodeUnderAvklaring") // TODO: debug dette
    void utfør_oppdaterAnmodningsperiode_ok() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Behandling forrigeBehandling = new Behandling();
        forrigeBehandling.setId(2L);
        forrigeBehandling.setFagsak(fagsak);
        forrigeBehandling.setType(Behandlingstyper.NY_VURDERING);
        forrigeBehandling.setRegistrertDato(Instant.now().minusSeconds(10));

        Behandling førsteBehandling = new Behandling();
        førsteBehandling.setId(3L);
        førsteBehandling.setFagsak(fagsak);
        førsteBehandling.setType(Behandlingstyper.FØRSTEGANG);
        førsteBehandling.setRegistrertDato(Instant.now().minusSeconds(20));
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


        verify(medlPeriodeService).oppdaterPeriodeUnderAvklaring(anmodningsperiode,  behandling.getId());
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
