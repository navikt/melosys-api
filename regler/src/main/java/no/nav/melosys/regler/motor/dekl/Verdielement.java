package no.nav.melosys.regler.motor.dekl;

import no.nav.melosys.regler.motor.KontekstManager;
import no.nav.melosys.regler.motor.Predikat;

public class Verdielement {
    
    private Verdielement() {} // Skal ikke instansieres direkte
    
    private Object verdi;
    
    public static Verdielement verdien(Object verdi) {
        Verdielement ve = new Verdielement();
        ve.verdi = verdi;
        return ve;
    }
    
    public static Verdielement variabelen(Object variabel) {
        return verdien(KontekstManager.hentVariabel(variabel));
    }

    public Predikat mangler() {
        return () -> {return verdi == null;};
    }

    public Predikat harVerdi() {
        return () -> {return verdi != null;};
    }

}
