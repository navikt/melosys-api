package no.nav.melosys.service.kontroll;

import java.util.function.Predicate;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;

public final class OverlappendeMedlemskapsperioderKontroller {

    private OverlappendeMedlemskapsperioderKontroller() {
    }

    public static boolean harOverlappendeMedlemsperiodeIkkeAvvistIPeriode(MedlemskapDokument medlemskapDokument,
                                                                          ErPeriode periode) {
        return overlappendeMedlemsperiodeMedPredikatFinnesIPeriode(medlemskapDokument,
            OverlappendeMedlemskapsperioderKontroller::erPeriodeIkkeAvvist, periode);
    }

    public static boolean harOverlappendeMedlemsperiodeGyldigIPeriode(MedlemskapDokument medlemskapDokument,
                                                                      Lovvalgsperiode lovvalgsperiode) {
        return overlappendeMedlemsperiodeMedPredikatFinnesIPeriode(medlemskapDokument,
            OverlappendeMedlemskapsperioderKontroller::erPeriodeGyldig, lovvalgsperiode);
    }

    private static boolean overlappendeMedlemsperiodeMedPredikatFinnesIPeriode(MedlemskapDokument medlemskapDokument,
                                                                               Predicate<Medlemsperiode> medlemsperiodeFilter,
                                                                               ErPeriode periode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> medlemsperiodeFilter.test(medlemsperiode) && PeriodeKontroller.periodeOverlapper(
                periode.getFom(), periode.getTom(), medlemsperiode.getPeriode().getFom(),
                medlemsperiode.getPeriode().getTom()));
    }

    private static boolean erPeriodeGyldig(Medlemsperiode medlemsperiode) {
        return PeriodestatusMedl.GYLD.getKode().equals(medlemsperiode.status);
    }

    private static boolean erPeriodeIkkeAvvist(Medlemsperiode medlemsperiode) {
        return !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status);
    }
}
