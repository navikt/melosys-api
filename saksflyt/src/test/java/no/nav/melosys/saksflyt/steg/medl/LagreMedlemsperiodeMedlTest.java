package no.nav.melosys.saksflyt.steg.medl;

import java.util.List;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.AVSLAATT;
import static no.nav.melosys.domain.kodeverk.InnvilgelsesResultat.INNVILGET;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LagreMedlemsperiodeMedlTest {

    public static final long BEHANDLING_ID = 123L;

    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlemskapsperiodeService medlemskapsperiodeService;


    private LagreMedlemsperiodeMedl lagreMedlemsperiodeMedl;

    private Prosessinstans prosessinstans;

    @BeforeEach
    void init() {
        lagreMedlemsperiodeMedl = new LagreMedlemsperiodeMedl(medlemskapsperiodeService, behandlingsresultatService);
        prosessinstans = lagProsessInstans();
    }

    @Test
    void utfør_ingenMedlemskapsperioder_erAvslag_gjørIngenting() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID))
            .thenReturn(lagBehandlingsresulat(emptyList()));


        lagreMedlemsperiodeMedl.utfør(prosessinstans);


        verifyNoInteractions(medlPeriodeService);
    }

    @Test
    void utfør_ingenInnvilgedeMedlemskapsperioder_erAvslag_gjørIngenting() {
        var medlemskapsperioder = List.of(lagMedlemskapsperiode(AVSLAATT), lagMedlemskapsperiode(AVSLAATT));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID))
            .thenReturn(lagBehandlingsresulat(medlemskapsperioder));


        lagreMedlemsperiodeMedl.utfør(prosessinstans);


        verifyNoInteractions(medlPeriodeService);
    }

    @Test
    void utfør_innvilgedeMedlemskapsperioder_oppretterEllerOppdatererMedlPerioder() {
        var medlemskapsperioder = List.of(lagMedlemskapsperiode(INNVILGET), lagMedlemskapsperiode(INNVILGET));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID))
            .thenReturn(lagBehandlingsresulat(medlemskapsperioder));


        lagreMedlemsperiodeMedl.utfør(lagProsessInstans());


        verify(medlemskapsperiodeService, times(2)).opprettEllerOppdaterMedlPeriode(eq(BEHANDLING_ID), any(Medlemskapsperiode.class));
    }

    @Test
    void utfør_avslutterMedlemskapsperioder_nårDetErNyVurderingOgInnvilgelse() {
        Medlemskapsperiode innvilgetMedlemskapsperiode = lagMedlemskapsperiode(INNVILGET);
        var medlemskapsperioder = List.of(lagMedlemskapsperiode(AVSLAATT), innvilgetMedlemskapsperiode);
        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(1L);
        Prosessinstans prosessinstans = lagProsessInstans();
        prosessinstans.getBehandling().setType(Behandlingstyper.NY_VURDERING);
        prosessinstans.getBehandling().setOpprinneligBehandling(opprinneligBehandling);
        Behandlingsresultat behandlingsresultat = lagBehandlingsresulat(medlemskapsperioder);
        behandlingsresultat.setBehandling(prosessinstans.getBehandling());
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        lagreMedlemsperiodeMedl.utfør(prosessinstans);

        verify(medlemskapsperiodeService).erstattMedlemskapsperioder(List.of(innvilgetMedlemskapsperiode), 1L, BEHANDLING_ID);
    }

    @Test
    void utfør_avslutterMedlemskapsperioder_nårDetErManglendeInnbetalingTrygdeavgiftOgViSkalIkkeOpphøre() {
        Medlemskapsperiode innvilgetMedlemskapsperiode = lagMedlemskapsperiode(INNVILGET);
        var medlemskapsperioder = List.of(innvilgetMedlemskapsperiode);
        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(1L);
        Prosessinstans prosessinstans = lagProsessInstans();
        prosessinstans.getBehandling().setType(Behandlingstyper.NY_VURDERING);
        prosessinstans.getBehandling().setOpprinneligBehandling(opprinneligBehandling);
        Behandlingsresultat behandlingsresultat = lagBehandlingsresulat(medlemskapsperioder);
        behandlingsresultat.setBehandling(prosessinstans.getBehandling());
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);

        lagreMedlemsperiodeMedl.utfør(prosessinstans);

        verify(medlemskapsperiodeService).erstattMedlemskapsperioder(List.of(innvilgetMedlemskapsperiode), 1L, BEHANDLING_ID);
    }

    @Test
    void utfør_avslutterMedlemskapsperioder_nårDetErManglendeInnbetalingTrygdeavgiftOgViSkalOpphøre() {
        Behandling opprinneligBehandling = new Behandling();
        opprinneligBehandling.setId(1L);
        Prosessinstans prosessinstans = lagProsessInstans();
        prosessinstans.getBehandling().setType(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT);
        prosessinstans.getBehandling().setOpprinneligBehandling(opprinneligBehandling);
        Behandlingsresultat behandlingsresultat = lagBehandlingsresulat(emptyList());
        behandlingsresultat.setBehandling(prosessinstans.getBehandling());
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(behandlingsresultat);


        lagreMedlemsperiodeMedl.utfør(prosessinstans);


        verify(medlemskapsperiodeService).erstattMedlemskapsperioder(emptyList(), 1L, BEHANDLING_ID);
    }

    private Prosessinstans lagProsessInstans() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        return prosessinstans;
    }


    private Behandlingsresultat lagBehandlingsresulat(List<Medlemskapsperiode> medlemskapsperioder) {
        MedlemAvFolketrygden medlemAvFolketrygden = new MedlemAvFolketrygden();
        medlemAvFolketrygden.setMedlemskapsperioder(medlemskapsperioder);
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.FØRSTEGANG);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN);
        behandlingsresultat.setMedlemAvFolketrygden(medlemAvFolketrygden);
        behandlingsresultat.setBehandling(behandling);

        return behandlingsresultat;
    }

    private Medlemskapsperiode lagMedlemskapsperiode(InnvilgelsesResultat innvilgelsesResultat) {
        var medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        return medlemskapsperiode;
    }
}
