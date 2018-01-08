package no.nav.melosys.regler.motor.voc;

import static no.nav.melosys.regler.motor.voc.FellesVokabular.utfør;

/**
 * Klasse som støtter deklarativ programmering av typen
 * 
 * hvis(predikat).så(kommando).ellers(kommando)
 * 
 */
public final class Deklarasjon {
    
    private Deklarasjon() {}
    
    private Predikat betingelse;
    
    public static final Deklarasjon hvis(Predikat betingelse) {
        Deklarasjon d = new Deklarasjon();
        d.betingelse = betingelse;
        return d;
    }

    public static final Deklarasjon hvis(Verdielement ve) {
        return Deklarasjon.hvis(ve.erSann());
    }

    public Deklarasjon så(Runnable... kommandoer) {
        if (betingelse.test()) {
            utfør(kommandoer);
        }
        return this;
    }
    
    public Deklarasjon ellersHvis(Predikat betingelse) {
        if (this.betingelse.test()) {
            return hvis(() -> false);
        } else {
            return hvis(betingelse);
        }
    }
    
    public void ellers(Runnable... kommandoer) {
        if (!betingelse.test()) {
            utfør(kommandoer);
        }
    }
    
}
