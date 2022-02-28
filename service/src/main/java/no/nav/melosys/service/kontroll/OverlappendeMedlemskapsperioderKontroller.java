package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
import java.util.function.Predicate;

import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;

public final class OverlappendeMedlemskapsperioderKontroller {

    private OverlappendeMedlemskapsperioderKontroller() {
    }

    public static boolean overlappendeMedlemsperiodeIkkeAvvistPeriode(LocalDate fom, LocalDate tom, MedlemskapDokument medlemskapDokument) {
        return overlappendeMedlemsperiodeMedPredikatFinnes(fom, tom, medlemskapDokument, OverlappendeMedlemskapsperioderKontroller::periodeIkkeAvvist);
    }

    public static boolean overlappendeMedlemsperiodeGyldigPeriode(LocalDate fom, LocalDate tom, MedlemskapDokument medlemskapDokument) {
        return overlappendeMedlemsperiodeMedPredikatFinnes(fom, tom, medlemskapDokument, OverlappendeMedlemskapsperioderKontroller::periodeGyldig);
    }

    private static boolean overlappendeMedlemsperiodeMedPredikatFinnes(LocalDate fom, LocalDate tom,
                                                                       MedlemskapDokument medlemskapDokument,
                                                                       Predicate<Medlemsperiode> medlemsperiodeFilter) {
        for (Medlemsperiode medlemsperiode : medlemskapDokument.hentMedlemsperioderKildeIkkeLånekassen()) {
            Periode periode = medlemsperiode.getPeriode();
            if (medlemsperiodeFilter.test(medlemsperiode) && PeriodeKontroller.periodeOverlapper(fom, tom,
                periode.getFom(), periode.getTom())) {
                return true;
            }
        }

        return false;
    }

    private static boolean periodeGyldig(Medlemsperiode medlemsperiode) {
        return PeriodestatusMedl.GYLD.getKode().equals(medlemsperiode.status);
    }

    private static boolean periodeIkkeAvvist(Medlemsperiode medlemsperiode) {
        return !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status);
    }
}
