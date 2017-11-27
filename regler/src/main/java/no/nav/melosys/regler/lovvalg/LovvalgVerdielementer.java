package no.nav.melosys.regler.lovvalg;

import java.time.temporal.ChronoUnit;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.regler.motor.dekl.Verdielement;

/**
 * Verbalisering av verdier som ikke beregnes med forretningsregler.
 */
public class LovvalgVerdielementer {

    private LovvalgVerdielementer() {}
    
    /** Antall måneder i søknadsperioden. */
    public static final Verdielement antallMånederI(ErPeriode periode) {
        return Verdielement.verdien(ChronoUnit.MONTHS.between(periode.getFom(), periode.getTom()));
    }

}
