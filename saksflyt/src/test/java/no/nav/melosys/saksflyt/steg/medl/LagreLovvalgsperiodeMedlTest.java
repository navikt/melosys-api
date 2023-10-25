package no.nav.melosys.saksflyt.steg.medl;

import java.util.NoSuchElementException;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflyt.TestdataFactory;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LagreLovvalgsperiodeMedlTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;
    @Captor
    private ArgumentCaptor<Lovvalgsperiode> lovvalgsperiodeArgumentCaptor;

    private LagreLovvalgsperiodeMedl lagreLovvalgsperiodeMedl;

    private final long behandlingID = 2434L;
    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final Behandling behandling = new Behandling();
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @BeforeEach
    public void setup() {
        lagreLovvalgsperiodeMedl = new LagreLovvalgsperiodeMedl(behandlingsresultatService, medlPeriodeService, saksbehandlingRegler);
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.TRYGDEAVTALE);
        fagsak.setTema(Sakstemaer.UNNTAK);

        behandling.setId(behandlingID);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);
    }

    @Test
    void utfør_erAvslagMedLovvalgsperiodeMedMedlID_avviserMedlPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.AVSLAATT);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);


        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).avvisPeriode(lovvalgsperiode.getMedlPeriodeID());
        verifyNoMoreInteractions(medlPeriodeService);
    }

    @Test
    void utfør_erInnvilgelseArt13IngenMedlID_oppretterForeløpigPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);


        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).opprettPeriodeForeløpig(lovvalgsperiode, behandlingID);
    }

    @Test
    void utfør_erInnvilgelseArt13MedMedlID_oppdatererTilForeløpigPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);


        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).oppdaterPeriodeForeløpig(lovvalgsperiode);
    }

    @Test
    void utfør_erInnvilgelseArt13IngenMedlIDUnntaksflyt_oppretterEndeligPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        when(saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)).thenReturn(true);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).opprettPeriodeEndelig(lovvalgsperiode, behandlingID);
    }

    @Test
    void utfør_erInnvilgelseArt13MedMedlIDUnntaksflyt_oppdatererTilEndeligPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        when(saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling)).thenReturn(true);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).oppdaterPeriodeEndelig(lovvalgsperiode);
    }

    @Test
    void utfør_erInnvilgelseArt12IngenMedlID_oppretterEndeligPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);


        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).opprettPeriodeEndelig(lovvalgsperiode, behandlingID);
    }

    @Test
    void utfør_erInnvilgelseArt12MedMedlID_oppdatererTilEndeligPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);


        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).oppdaterPeriodeEndelig(lovvalgsperiode);
    }

    @Test
    void utfør_nyVurderingOgPeriodeFinnes_oppdaterPeriode() {
        Behandling behandling = TestdataFactory.lagBehandlingNyVurdering();
        prosessinstans.setBehandling(behandling);
        Behandling opprinneligBehandling = TestdataFactory.lagBehandling();
        behandling.setOpprinneligBehandling(opprinneligBehandling);
        Behandlingsresultat opprinneligResultat = new Behandlingsresultat();
        Lovvalgsperiode opprinneligLovvalgsperiode = lagLovvalgsperiode(777L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            InnvilgelsesResultat.INNVILGET);
        opprinneligResultat.getLovvalgsperioder().add(opprinneligLovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.getId())).thenReturn(opprinneligResultat);
        Lovvalgsperiode nyLovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(nyLovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);


        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).oppdaterPeriodeEndelig(lovvalgsperiodeArgumentCaptor.capture());
        assertThat(lovvalgsperiodeArgumentCaptor.getValue().getMedlPeriodeID()).isEqualTo((opprinneligLovvalgsperiode.getMedlPeriodeID()));
    }

    @Test
    void utfør_nyVurderingOgPeriodeFinnesIkke_opprettPeriode() {
        Behandling behandling = TestdataFactory.lagBehandlingNyVurdering();
        prosessinstans.setBehandling(behandling);
        Behandling opprinneligBehandling = TestdataFactory.lagBehandling();
        behandling.setOpprinneligBehandling(opprinneligBehandling);
        Behandlingsresultat opprinneligResultat = new Behandlingsresultat();

        when(behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.getId())).thenReturn(opprinneligResultat);
        Lovvalgsperiode nyLovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(nyLovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);


        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).opprettPeriodeEndelig(nyLovvalgsperiode, behandling.getId());
    }

    @Test
    void utfør_nyVurderingOgOpprinneligBehandlingFinnesIkke_opprettPeriode() {
        Behandling behandling = TestdataFactory.lagBehandlingNyVurdering();
        prosessinstans.setBehandling(behandling);
        behandling.setOpprinneligBehandling(null);

        Lovvalgsperiode nyLovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(nyLovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);


        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verify(medlPeriodeService).opprettPeriodeEndelig(nyLovvalgsperiode, behandling.getId());
    }

    @Test
    void utfør_ikkeGodkjentRegistreringUnntak_oppretterIkkeLovvalgsperiode() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.TRYGDEAVTALE);
        fagsak.setTema(Sakstemaer.UNNTAK);

        Behandling behandling = TestdataFactory.lagBehandling();
        behandling.setFagsak(fagsak);
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK);
        prosessinstans.setBehandling(behandling);

        behandlingsresultat.setUtfallRegistreringUnntak(Utfallregistreringunntak.IKKE_GODKJENT);
        when(behandlingsresultatService.hentBehandlingsresultat(behandling.getId())).thenReturn(behandlingsresultat);
        when(saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any())).thenReturn(true);


        lagreLovvalgsperiodeMedl.utfør(prosessinstans);


        verifyNoInteractions(medlPeriodeService);
    }

    @Test
    void utfør_typeFastsattLovvalgslandIngenLovvalgsperiode_forventException() {
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);

        assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> lagreLovvalgsperiodeMedl.utfør(prosessinstans))
            .withMessageContaining("Ingen lovvalgsperiode");
    }

    @Test
    void utfør_lovvalgsperiodeFinnesInnvilgelsesresultatDelvisInnvilget_forventException() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.DELVIS_INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> lagreLovvalgsperiodeMedl.utfør(prosessinstans))
            .withMessageContaining("Ukjent eller ikke-eksisterende innvilgelsesresultat");
    }

    private Lovvalgsperiode lagLovvalgsperiode(Long medlPeriodeID,
                                               LovvalgBestemmelse lovvalgBestemmelse,
                                               InnvilgelsesResultat innvilgelsesResultat) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setMedlPeriodeID(medlPeriodeID);
        lovvalgsperiode.setBestemmelse(lovvalgBestemmelse);
        lovvalgsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        return lovvalgsperiode;
    }
}
