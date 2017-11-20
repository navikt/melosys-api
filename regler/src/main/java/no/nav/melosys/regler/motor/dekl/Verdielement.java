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

    @SuppressWarnings("unused") 
    public static Verdielement antallet(Iterable<?> settet) {
        int ant = 0;
        for (Object t : settet) {
            ant++;
        }
        return Verdielement.verdien(ant);
    }
    
    public Predikat mangler() {
        return () -> {return verdi == null;};
    }

    public Predikat harVerdi() {
        return () -> {return verdi != null;};
    }
    
    public Predikat erLik(Object annetVerdi) {
        return () -> {return verdi.equals(annetVerdi);}; // Ja, vi vil ha teknisk feil / NPE hvis verdi er null
    }

    public Predikat erEnAv(Object... andreVerdier) {
        return () -> {
            for (Object annetVerdi : andreVerdier) {
                if (verdi.equals(annetVerdi)) { // Ja, vi vil ha teknisk feil / NPE hvis verdi er null
                    return true;
                }
            }
            return false;
        };
    }
    
    @SuppressWarnings("unchecked")
    public <T> Predikat erStørreEnn(T grense) {
        return () -> {
            return sammenliknMed(grense) > 0;
        };
    }

    public Predikat erStørreEnnEllerLik(Object grense) {
        return () -> {
            return sammenliknMed(grense) >= 0;
        };
    }
    
    @SuppressWarnings("unchecked")
    private int sammenliknMed(Object grense) {
        try {
            return ((Comparable<Object>) verdi).compareTo(grense);
        } catch (ClassCastException e) {
            throw new RuntimeException("Kan ikke sammenligne en " + verdi.getClass() + " med en " + grense.getClass());
        }
    }

}
