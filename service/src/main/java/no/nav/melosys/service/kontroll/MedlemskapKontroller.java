package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;

public final class MedlemskapKontroller {

    private MedlemskapKontroller() {
    }

    public static boolean lovvalgslandErNorge(Landkoder landkode) {
        return landkode.equals(Landkoder.NO);
    }

    public static boolean overlappendeMedlemsperiodeIkkeAvvistPeriode(LocalDate fom, LocalDate tom, MedlemskapDokument medlemskapDokument) {
        return overlappendeMedlemsperiodeMedPredikatFinnes(fom, tom, medlemskapDokument, MedlemskapKontroller::periodeIkkeAvvist);
    }

    public static boolean overlappendeMedlemsperiodeGyldigPeriode(LocalDate fom, LocalDate tom, MedlemskapDokument medlemskapDokument) {
        return overlappendeMedlemsperiodeMedPredikatFinnes(fom, tom, medlemskapDokument, MedlemskapKontroller::periodeGyldig);
    }

    private static boolean overlappendeMedlemsperiodeMedPredikatFinnes(LocalDate fom, LocalDate tom, MedlemskapDokument medlemskapDokument, Predicate<Medlemsperiode> medlemsperiodeFilter) {
        for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
            Periode periode = medlemsperiode.getPeriode();
            if (medlemsperiodeFilter.test(medlemsperiode) && PeriodeKontroller.periodeOverlapper(fom, tom, periode.getFom(), periode.getTom())) {
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

    public static boolean statsborgerskapIkkeMedlemsland(Collection<String> statsborgerskapLandkoder) {
        return !statsborgerskapLandkoder.isEmpty() && Arrays.stream(Landkoder.values())
            .noneMatch(landkode -> statsborgerskapLandkoder.contains(landkode.getKode()));
    }
}
