package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.motor.voc.Verdielement.verdien;

import java.time.temporal.ChronoUnit;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.regler.motor.voc.Verdielement;

/**
 * Verbalisering av verdier som ikke beregnes med forretningsregler.
 */
public class LovvalgVerdielementer {

    private LovvalgVerdielementer() {}
    
    /** Antall måneder i en perioden, rundet av oppover. */
    public static final Verdielement antallMånederI(ErPeriode periode) {
        return verdien(ChronoUnit.MONTHS.between(periode.getFom(), periode.getTom().plusMonths(1)));
    }

}
