package no.nav.melosys.service.unntaksperiode.kontroll;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Unntak_periode_begrunnelser;

final class PeriodeKontroller {

    private PeriodeKontroller() {
    }

    static Unntak_periode_begrunnelser gyldigPeriode(KontrollData kontrollData) {
        Periode periode = kontrollData.sedDokument.getLovvalgsperiode();
        return periode.getTom() != null && periode.getFom().isAfter(periode.getTom()) ?
            Unntak_periode_begrunnelser.FEIL_I_PERIODEN : null;
    }

    static Unntak_periode_begrunnelser periodeErÅpen(KontrollData kontrollData) {
        return kontrollData.sedDokument.getLovvalgsperiode().getTom() == null ?
            Unntak_periode_begrunnelser.INGEN_SLUTTDATO : null;
    }

    static Unntak_periode_begrunnelser periodeMaks24Mnd(KontrollData kontrollData) {
        Periode periode = kontrollData.sedDokument.getLovvalgsperiode();
        return periode.getTom() != null && ChronoUnit.MONTHS.between(periode.getFom(), periode.getTom()) >= 23L ?
            Unntak_periode_begrunnelser.PERIODEN_OVER_24_MD : null;
    }

    static Unntak_periode_begrunnelser periodeEldreEnn5År(KontrollData kontrollData) {
        Periode periode = kontrollData.sedDokument.getLovvalgsperiode();
        return periode.getFom().isBefore(LocalDate.now().minusYears(5L)) ?
            Unntak_periode_begrunnelser.PERIODE_FOR_GAMMEL : null;
    }

    static Unntak_periode_begrunnelser periodeOver1ÅrFremITid(KontrollData kontrollData) {
        Periode periode = kontrollData.sedDokument.getLovvalgsperiode();
        return periode.getFom().isAfter(LocalDate.now().plusYears(1L)) ?
            Unntak_periode_begrunnelser.PERIODE_LANGT_FREM_I_TID : null;
    }
}
