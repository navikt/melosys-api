package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;
import java.util.Arrays;

import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;

final class MedlemskapKontroller {

    private MedlemskapKontroller() {
    }

    static Unntak_periode_begrunnelser lovvalgslandErNorge(KontrollData kontrollData) {
        Landkoder landkode = kontrollData.sedDokument.getLovvalgslandKode();
        return landkode.equals(Landkoder.NO) ?
            Unntak_periode_begrunnelser.LOVVALGSLAND_NORGE : null;
    }

    static Unntak_periode_begrunnelser overlappendeMedlemsperiode(KontrollData kontrollData) {

        LocalDate fom = kontrollData.sedDokument.getLovvalgsperiode().getFom();
        LocalDate tom = kontrollData.sedDokument.getLovvalgsperiode().getTom();
        MedlemskapDokument medlemskapDokument = kontrollData.medlemskapDokument;

        if (tom == null) {
            for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
                Periode periode = medlemsperiode.getPeriode();
                if (fom.isBefore(periode.getTom())) return Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER;
            }
        } else {
            for (Medlemsperiode medlemsperiode : medlemskapDokument.getMedlemsperiode()) {
                Periode periode = medlemsperiode.getPeriode();
                if (fom.isBefore(periode.getTom()) && periode.getFom().isBefore(tom))
                    return Unntak_periode_begrunnelser.OVERLAPPENDE_MEDL_PERIODER;
            }
        }
        return null;
    }

    static Unntak_periode_begrunnelser statsborgerskapIkkeMedlemsland(KontrollData kontrollData) {
        SedDokument sedDokument = kontrollData.sedDokument;
        return Arrays.stream(Landkoder.values())
            .noneMatch(landkode -> sedDokument.getStatsborgerskapKoder().contains(landkode.getKode()))
            ? Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND : null;
    }
}
