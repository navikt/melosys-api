package no.nav.melosys.service.kontroll.vedtak;

import java.time.LocalDate;
import java.util.Collection;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.TekniskException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VedtakKontrollServiceTest {

    private Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private MedlemskapDokument medlemskapDokument = new MedlemskapDokument();

    private Behandling behandling = new Behandling();
    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();

    private VedtakKontrollService vedtakKontrollService = new VedtakKontrollService();

    @Before
    public void setup() {
        Saksopplysning medlSaksopplysning = new Saksopplysning();
        medlSaksopplysning.setType(SaksopplysningType.MEDL);
        medlSaksopplysning.setDokument(medlemskapDokument);

        behandling.getSaksopplysninger().add(medlSaksopplysning);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);
    }

    @Test
    public void utførKontroller_periodeUnder24MndArt12IkkeOverlappendePeriode_returnererTomColleciton() throws TekniskException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(1));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        Collection<Unntak_periode_begrunnelser> resultat = vedtakKontrollService.utførKontroller(behandling, behandlingsresultat);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void utførKontroller_periodeOver24MndArt16IkkeOverlappendePeriode_returnererTomColleciton() throws TekniskException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(3));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
        Collection<Unntak_periode_begrunnelser> resultat = vedtakKontrollService.utførKontroller(behandling, behandlingsresultat);
        assertThat(resultat).isEmpty();
    }

    @Test
    public void utførKontroller_periodeOver24MndArt12MedOverlappendePeriode_returnererCollectionMedToKoder() throws TekniskException {
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now().plusYears(2));
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.periode = new Periode(LocalDate.now().plusMonths(2), LocalDate.now().plusYears(2));
        medlemskapDokument.getMedlemsperiode().add(medlemsperiode);

        Collection<Unntak_periode_begrunnelser> resultat = vedtakKontrollService.utførKontroller(behandling, behandlingsresultat);
        assertThat(resultat).containsExactlyInAnyOrder(Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER, Unntak_periode_begrunnelser.PERIODEN_OVER_24_MD);
    }
}