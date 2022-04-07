package no.nav.melosys.service.kontroll;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;

public final class OverlappendeMedlemskapsperioderKontroller {

    private OverlappendeMedlemskapsperioderKontroller() {
    }

    public static boolean harOverlappendeMedlemsperiodeFraSed(MedlemskapDokument medlemskapDokument,
                                                              ErPeriode kontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status)
                && PeriodeKontroller.periodeOverlapperMedInklusivPerioder(medlemsperiode.getPeriode(), kontrollperiode));
    }

    public static boolean harOverlappendeMedlemsperiode(MedlemskapDokument medlemskapDokument,
                                                        Lovvalgsperiode kontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status)
                && PeriodeKontroller.periodeOverlapper(medlemsperiode.getPeriode(), kontrollperiode)
                && (kontrollperiode.erNyPeriodeForMedl() || !kontrollperiode.harSammeMedlID(medlemsperiode.id)));
    }
}
