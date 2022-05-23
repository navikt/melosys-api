package no.nav.melosys.service.kontroll.overlapp;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import no.nav.melosys.domain.ErPeriode;

import static java.util.Objects.isNull;

public class PeriodeOverlappSjekk {

    private final LocalDate fom1;
    private final LocalDate tom1;
    private final LocalDate fom2;
    private final LocalDate tom2;

    public PeriodeOverlappSjekk(ErPeriode periode1, ErPeriode periode2) {
        fom1 = periode1.getFom();
        tom1 = periode1.getTom();
        fom2 = periode2.getFom();
        tom2 = periode2.getTom();

        validerFraOgMedDato();
        validerTilOgMedDato();
    }

    public boolean harPeriodeSomOverlapperMerEnn1Dag() {
        if (isNull(tom1)) {
            return åpenPeriodeOverlapper(fom2, tom2, fom1);
        }

        if (isNull(tom2)) {
            return åpenPeriodeOverlapper(fom1, tom1, fom2);
        }

        return periodeOverlapperMerEnn1Dag();
    }

    private boolean periodeOverlapperMerEnn1Dag() {
        if (harPeriodeSomOverlapper()) {
            LocalDate startTidNærmestSluttTid = fom1.isBefore(fom2) ? fom2 : fom1;
            LocalDate sluttTidNærmestStartTid = tom1.isBefore(tom2) ? tom1 : tom2;
            long dagerOverlap = ChronoUnit.DAYS.between(startTidNærmestSluttTid, sluttTidNærmestStartTid);

            LocalDate startTidLengstVekkFraSluttTid = fom1.isBefore(fom1) ? fom1 : fom2;
            LocalDate sluttTidLengstVekkFraStartTid = tom1.isAfter(tom2) ? tom1 : tom2;

            if (harPeriodeNesteÅr(startTidLengstVekkFraSluttTid, sluttTidLengstVekkFraStartTid)) {
                return overlapperSammeDagNesteÅrEllerStørre(dagerOverlap);
            }
            return overlapperMerEnnEnDagSammeÅr(dagerOverlap);
        }
        return false;
    }

    private boolean overlapperMerEnnEnDagSammeÅr(long dagerOverlap) {
        return dagerOverlap > 1;
    }

    private boolean harPeriodeNesteÅr(LocalDate startTid, LocalDate sluttTid) {
        return ChronoUnit.YEARS.between(startTid, sluttTid) >= 1;
    }

    private boolean overlapperSammeDagNesteÅrEllerStørre(long dagerOverlap) {
        return dagerOverlap >= 1;
    }


//    private boolean harOverEnDagOverlap() {
//        // fom/tom1 surrounds fom/tom2
//        if ((fom1.isEqual(fom2) || fom1.isBefore(fom2)) && (tom1.isEqual(tom2) || tom1.isAfter(tom2))) {
//            var daysOverlap = ChronoUnit.DAYS.between(fom2, tom2);
//            return daysOverlap > 1;
//        }
//
//        // fom/tom2 surrounds fom/tom1
//        if ((fom2.isEqual(fom1) || fom2.isBefore(fom1)) && (tom2.isEqual(tom1) || tom2.isAfter(tom1))) {
//            var daysOverlap = ChronoUnit.DAYS.between(fom1, tom1);
//            return daysOverlap > 1;
//        }
//
//        return true;
//
//    }

    private boolean harPeriodeSomOverlapper() {
        return ((fom1 == null || tom2 == null || (fom1.isBefore(tom2) || fom1.isEqual(tom2)))
            && (fom2 == null || tom1 == null || (fom2.isBefore(tom1) || fom2.isEqual(tom1)))
            && (fom1 == null || tom1 == null || (fom1.isBefore(tom1) || fom1.isEqual(tom1)))
            && (fom2 == null || tom2 == null || (fom2.isBefore(tom2) || fom2.isEqual(tom2)))
        );
    }

    private void validerTilOgMedDato() {
        if (isNull(tom1) && isNull(tom2)) {
            throw new IllegalArgumentException("Kan ikke ha to åpne perioder samtidig");
        }
    }

    private void validerFraOgMedDato() {
        if (isNull(fom1) || isNull(fom2)) {
            throw new IllegalArgumentException("Fom-dato kan ikke være null!");
        }
    }

    private boolean harLikeStartDatoerEllerSluttDatoer() {
        return datoErLik(fom1, fom2) || datoErLik(tom1, tom2);
    }

    private boolean datoErLik(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return date1 == date2;
        }
        return date1.equals(date2);
    }

    private boolean åpenPeriodeOverlapper(LocalDate fom, LocalDate tom, LocalDate åpenPeriode) {
        return fom.isAfter(åpenPeriode) && tom.isAfter(åpenPeriode);
    }
}
