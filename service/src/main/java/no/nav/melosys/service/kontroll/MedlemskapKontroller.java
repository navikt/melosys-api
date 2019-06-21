package no.nav.melosys.service.kontroll;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

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

    public static boolean overlappendeMedlemsperiode(LocalDate fom, LocalDate tom, MedlemskapDokument medlemskapDokument) {

        if (tom == null) {
            for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
                Periode periode = medlemsperiode.getPeriode();
                if (periodeIkkeAvvist(medlemsperiode) && fom.isBefore(periode.getTom())) {
                    return true;
                }
            }
        } else {
            for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
                Periode periode = medlemsperiode.getPeriode();
                if (periodeIkkeAvvist(medlemsperiode) && fom.isBefore(periode.getTom()) && periode.getFom().isBefore(tom)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean periodeIkkeAvvist(Medlemsperiode medlemsperiode) {
        return !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status);
    }

    public static boolean statsborgerskapIkkeMedlemsland(Collection<String> statsborgerskapLandkoder) {
        return !statsborgerskapLandkoder.isEmpty() && Arrays.stream(Landkoder.values())
            .noneMatch(landkode -> statsborgerskapLandkoder.contains(landkode.getKode()));
    }
}
