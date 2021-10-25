package no.nav.melosys.saksflyt.steg.medl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
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
    private LocalDate NOW = LocalDate.now();

    @BeforeEach
    public void setUp() {
        lagreAnmodningsperiodeIMedl = new LagreAnmodningsperiodeIMedl(behandlingsresultatService, medlPeriodeService);

        prosessinstans = new Prosessinstans();

        Behandling behandling = new Behandling();
        behandling.setId(1L);

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(null, null, Landkoder.CH,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, null, null, null, Trygdedekninger.FULL_DEKNING_EOSFO);

        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        prosessinstans.setBehandling(behandling);
        prosessinstans.getBehandling().setType(Behandlingstyper.SOEKNAD);
        prosessinstans.setType(ProsessType.ANMODNING_OM_UNNTAK);
    }

    @Test
    void utførNårBehandlingsresultatTypeErAnmodning_om_unntak() {
        behandlingsresultat.setType(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        behandlingsresultat.setAnmodningsperioder(lagAnmodningsperioderMedDato(NOW, NOW.plusMonths(1)));

        lagreAnmodningsperiodeIMedl.utfør(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeUnderAvklaring(any(Anmodningsperiode.class), anyLong(), eq(false));
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
        verify(medlPeriodeService, never()).opprettPeriodeUnderAvklaring(any(Anmodningsperiode.class), anyLong(), anyBoolean());
    }

    private Set<Anmodningsperiode> lagAnmodningsperioderMedDato(LocalDate fom, LocalDate tom) {
        return Set.of(new Anmodningsperiode(fom, tom, null, null, null, null, null, null));
    }
}
