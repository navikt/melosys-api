package no.nav.melosys.service.kontroll.overlapp;

import java.time.LocalDate;

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
        if (harLikeStartDatoerEllerSluttDatoer()) {
            return true;
        }

        if (isNull(tom1)) {
            return åpenPeriodeOverlapper(fom2, tom2, fom1);
        }

        if (isNull(tom2)) {
            return åpenPeriodeOverlapper(fom1, tom1, fom2);
        }

        return periodeOverlapperMerEnn1Dag();
    }

    private boolean periodeOverlapperMerEnn1Dag() {
        return fom1.isBefore(fom2) && tom1.isAfter(fom2) || fom1.isBefore(tom2) && fom1.isAfter(fom2);
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
