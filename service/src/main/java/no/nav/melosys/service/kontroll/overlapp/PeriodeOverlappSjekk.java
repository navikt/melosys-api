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
            return åpenPeriodeOverlapperMerEnn1Dag(fom2, tom2, fom1);
        }

        if (isNull(tom2)) {
            return åpenPeriodeOverlapperMerEnn1Dag(fom1, tom1, fom2);
        }

        return periodeOverlapperMerEnn1Dag();
    }

    private boolean periodeOverlapperMerEnn1Dag() {
        if (harPeriodeSomOverlapper()) {
            return hentAntallDagerOverlapp() > 0;
        }
        return false;
    }

    private long hentAntallDagerOverlapp() {
        LocalDate senestStartDato = fom1.isBefore(fom2) ? fom2 : fom1;
        LocalDate tidligstSluttDato = tom1.isBefore(tom2) ? tom1 : tom2;
        return ChronoUnit.DAYS.between(senestStartDato, tidligstSluttDato);
    }

    private boolean harPeriodeSomOverlapper() {
        return (tom2 == null || (fom1.isBefore(tom2) || fom1.isEqual(tom2))) && (tom1 == null || (tom1.isAfter(fom2) || tom1.isEqual(fom2)));
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

    private boolean åpenPeriodeOverlapperMerEnn1Dag(LocalDate fom, LocalDate tom, LocalDate åpenFom) {
        return fom.isAfter(åpenFom) || tom.isAfter(åpenFom);
    }
}
