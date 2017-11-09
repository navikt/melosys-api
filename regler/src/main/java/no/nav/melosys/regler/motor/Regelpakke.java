package no.nav.melosys.regler.motor;

import no.nav.melosys.regler.motor.dekl.Deklarasjon;

/**
 * Superklasse for regelpakker.
 * 
 * Subklassene skal bare trenge å implementere en eller flere metoder som annoteres med @Regel
 * 
 */
public abstract class Regelpakke {
    
    /**
     * Støttefunksjon som utfører alle deklarasjoner den får
     */
    public static void utfør(Deklarasjon... deklarasjoner) {
        for (Deklarasjon d : deklarasjoner) {
            d.utfør();
        }
    }

}
