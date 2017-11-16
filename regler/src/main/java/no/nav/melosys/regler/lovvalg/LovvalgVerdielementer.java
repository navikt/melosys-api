package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknadDokumentet;

import java.time.temporal.ChronoUnit;

import no.nav.melosys.regler.motor.dekl.Verdielement;

/**
 * Verbalisering av verdier som ikke beregnes med forretningsregler.
 */
public class LovvalgVerdielementer {

    private LovvalgVerdielementer() {}
    
    /** Antall måneder i søknadsperioden. */
    public static final Verdielement antallMånederISøknadsPerioden() {
        return Verdielement.verdien(ChronoUnit.MONTHS.between(søknadDokumentet().periode.getFom(), søknadDokumentet().periode.getTom()));
    }

}
