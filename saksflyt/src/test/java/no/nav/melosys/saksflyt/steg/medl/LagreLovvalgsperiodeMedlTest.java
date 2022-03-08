package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
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
class LagreLovvalgsperiodeMedlTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private MedlPeriodeService medlPeriodeService;

    private LagreLovvalgsperiodeMedl lagreLovvalgsperiodeMedl;

    private final long behandlingID = 2434L;
    private final Prosessinstans prosessinstans = new Prosessinstans();
    private final Behandling behandling = new Behandling();
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();


    @BeforeEach
    public void setup() {
        lagreLovvalgsperiodeMedl = new LagreLovvalgsperiodeMedl(behandlingsresultatService, medlPeriodeService);

        behandling.setId(behandlingID);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        prosessinstans.setBehandling(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
    }

    @Test
    void utfør_erAvslagMedLovvalgsperiodeMedMedlID_avviserMedlPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.AVSLAATT);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).avvisPeriode(eq(lovvalgsperiode.getMedlPeriodeID()));
        verifyNoMoreInteractions(medlPeriodeService);
    }

    @Test
    void utfør_erInnvilgelseArt13IngenMedlID_oppretterForeløpigPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeForeløpig(eq(lovvalgsperiode), eq(behandlingID), eq(false));
    }

    @Test
    void utfør_erInnvilgelseArt13MedMedlID_oppdatererTilForeløpigPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).oppdaterPeriodeForeløpig(eq(lovvalgsperiode), eq(false));
    }

    @Test
    void utfør_erInnvilgelseArt12IngenMedlID_oppretterEndeligPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeEndelig(eq(lovvalgsperiode), eq(behandlingID), eq(false));
    }

    @Test
    void utfør_erInnvilgelseArt12MedMedlID_oppdatererTilEndeligPeriode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(false));
    }

    @Test
    void utfør_avslagManglendeOpplysningerIngenLovvalgsperiode_oppretterIkkeLovvalgsperiode() {
        behandlingsresultat.setType(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verifyNoInteractions(medlPeriodeService);
    }

    @Test
    void utfør_typeFastsattLovvalgslandIngenLovvalgsperiode_forventException() {
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> lagreLovvalgsperiodeMedl.utfør(prosessinstans))
            .withMessageContaining("Finner ingen lovvalgsperiode");
    }

    @Test
    void utfør_lovvalgsperiodeFinnesInnvilgelsesresultatDelvisInnvilget_forventException() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.DELVIS_INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> lagreLovvalgsperiodeMedl.utfør(prosessinstans))
            .withMessageContaining("Ukjent eller ikke-eksisterende innvilgelsesresultat");
    }

    @Test
    void utfør_erInnvilgelseNyVurdering_erstatterOgLagrerNyLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandling.setType(Behandlingstyper.NY_VURDERING);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).avvisPeriode(eq(lovvalgsperiode.getMedlPeriodeID()));
        verify(medlPeriodeService).opprettPeriodeEndelig(eq(lovvalgsperiode), eq(behandlingID), eq(false));
        verifyNoMoreInteractions(medlPeriodeService);
    }

    @Test
    void utfør_erAvslagNyVurdering_erstatterOgLagrerNyLovvalgsperiode() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1, InnvilgelsesResultat.AVSLAATT);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        behandling.setType(Behandlingstyper.NY_VURDERING);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).avvisPeriode(eq(lovvalgsperiode.getMedlPeriodeID()));
        verify(medlPeriodeService).opprettPeriodeEndelig(eq(lovvalgsperiode), eq(behandlingID), eq(false));
        verifyNoMoreInteractions(medlPeriodeService);
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
