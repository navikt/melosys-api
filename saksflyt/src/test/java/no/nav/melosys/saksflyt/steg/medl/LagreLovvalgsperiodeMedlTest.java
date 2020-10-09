package no.nav.melosys.saksflyt.steg.medl;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.medl.MedlPeriodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    public void setup() throws IkkeFunnetException {
        lagreLovvalgsperiodeMedl = new LagreLovvalgsperiodeMedl(behandlingsresultatService, medlPeriodeService);

        behandling.setId(behandlingID);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        prosessinstans.setBehandling(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
    }

    @Test
    void utfør_erAvslagMedLovvalgsperiodeMedMedlID_avviserMedlPeriode() throws MelosysException {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.AVSLAATT);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).avvisPeriode(eq(lovvalgsperiode.getMedlPeriodeID()));
        verifyNoMoreInteractions(medlPeriodeService);
    }

    @Test
    void utfør_erInnvilgelseArt13IngenMedlID_oppretterForeløpigPeriode() throws MelosysException {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeForeløpig(eq(lovvalgsperiode), eq(behandlingID), eq(false));
    }

    @Test
    void utfør_erInnvilgelseArt13MedMedlID_oppdatererTilForeløpigPeriode() throws MelosysException {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).oppdaterPeriodeForeløpig(eq(lovvalgsperiode), eq(false));
    }

    @Test
    void utfør_erInnvilgelseArt12IngenMedlID_oppretterEndeligPeriode() throws MelosysException {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(null, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).opprettPeriodeEndelig(eq(lovvalgsperiode), eq(behandlingID), eq(false));
    }

    @Test
    void utfør_erInnvilgelseArt12MedMedlID_oppdatererTilEndeligPeriode() throws MelosysException {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(11L, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verify(medlPeriodeService).oppdaterPeriodeEndelig(eq(lovvalgsperiode), eq(false));
    }

    @Test
    void utfør_avslagManglendeOpplysningerIngenLovvalgsperiode_oppretterIkkeLovvalgsperiode() throws MelosysException {
        behandlingsresultat.setType(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        lagreLovvalgsperiodeMedl.utfør(prosessinstans);
        verifyNoInteractions(medlPeriodeService);
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