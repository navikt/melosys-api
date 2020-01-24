package no.nav.melosys.service.kontroll.vedtak;

import java.time.LocalDate;
import java.util.Collection;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.LovvalgsperiodeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VedtakKontrollServiceTest {

    private Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private MedlemskapDokument medlemskapDokument = new MedlemskapDokument();

    private Behandling behandling = new Behandling();
    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private final long behandlingID = 33L;

    private VedtakKontrollService vedtakKontrollService;

    @Before
    public void setup() throws FunksjonellException {
        Saksopplysning medlSaksopplysning = new Saksopplysning();
        medlSaksopplysning.setType(SaksopplysningType.MEDL);
        medlSaksopplysning.setDokument(medlemskapDokument);

        when(behandlingService.hentBehandling(eq(behandlingID))).thenReturn(behandling);
        when(lovvalgsperiodeService.hentValidertLovvalgsperiode(eq(behandlingID))).thenReturn(lovvalgsperiode);

        behandling.getSaksopplysninger().add(medlSaksopplysning);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        vedtakKontrollService = new VedtakKontrollService(behandlingService, lovvalgsperiodeService);
    }

    @Test
    public void utførKontroller_periodeUnder24MndArt12IkkeOverlappendePeriode_returnererTomColleciton() throws TekniskException, FunksjonellException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        Collection<Kontroll_begrunnelser> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void utførKontroller_periodeOver24MndArt16IkkeOverlappendePeriode_returnererTomColleciton() throws TekniskException, FunksjonellException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        Collection<Kontroll_begrunnelser> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void utførKontroller_periodeOver24MndArt12MedOverlappendePeriode_returnererCollectionMedToKoder() throws TekniskException, FunksjonellException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(2));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(LocalDate.now().plusMonths(2), LocalDate.now().plusYears(2));
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemskapDokument.getMedlemsperiode().add(medlemsperiode);

        Collection<Kontroll_begrunnelser> resultat = vedtakKontrollService.utførKontroller(behandlingID, Vedtakstyper.FØRSTEGANGSVEDTAK);
        assertThat(resultat).containsExactlyInAnyOrder(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, Kontroll_begrunnelser.PERIODEN_OVER_24_MD);
    }
}