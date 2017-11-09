package no.nav.melosys.regler.motor.dekl;

import no.nav.melosys.regler.motor.KontekstManager;
import no.nav.melosys.regler.motor.Predikat;

/**
 * Klasse som støtter deklarativ programmering av typen
 * hvis(predikat).så(kommando).ellers(kommando)
 * 
 * Klassen implementerer Runable, slik at kommandoene kan være (nøstede) deklarasjoner.
 */
public final class Deklarasjon implements Runnable {
    
    private Deklarasjon() {}
    
    private Predikat betingelse;
    
    private Runnable[] såKommando;
    
    private Runnable[] ellersKommando;
    
    public static Deklarasjon hvis(Predikat p) {
        Deklarasjon d = new Deklarasjon();
        d.betingelse = p;
        return d;
    }

    public Deklarasjon så(Runnable... kommando) {
        this.såKommando = kommando;
        return this;
    }
    
    public Deklarasjon ellers(Runnable... kommando) {
        this.ellersKommando = kommando;
        return this;
    }
    
    public void utfør() {
        if (betingelse.test()) {
            if (såKommando != null) {
                for (Runnable kommando : såKommando) {
                    kommando.run();
                }
            }
        } else {
            if (ellersKommando != null) {
                for (Runnable kommando : ellersKommando) {
                    kommando.run();
                }
            }
        }
    }

    @Override
    public void run() {
        utfør();
    }
    
}
