package no.nav.melosys.regler.motor.dekl;

/**
 * Klasse som støtter deklarativ programmering av typen
 * 
 * hvis(predikat).så(kommando).ellers(kommando)
 * 
 */
public final class Deklarasjon {
    
    private Deklarasjon() {}
    
    private Predikat betingelse;
    
    public static Deklarasjon hvis(Predikat p) {
        Deklarasjon d = new Deklarasjon();
        d.betingelse = p;
        return d;
    }

    public static Deklarasjon hvis(Verdielement ve) {
        return Deklarasjon.hvis(ve.erSann());
    }

    public Deklarasjon så(Runnable... kommandoer) {
        if (betingelse.test()) {
            utfør(kommandoer);
        }
        return this;
    }
    
    public Deklarasjon ellers(Runnable... kommandoer) {
        if (!betingelse.test()) {
            utfør(kommandoer);
        }
        return this;
    }
    
    
    
    private void utfør(Runnable... kommandoer) {
        for (Runnable kommando : kommandoer) {
            kommando.run();
        }
    }

}
