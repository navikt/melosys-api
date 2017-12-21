package no.nav.melosys.regler.motor.voc;

import org.springframework.util.Assert;

import no.nav.melosys.regler.motor.KontekstManager;

public class Verdielement {
    
    private Verdielement() {} // Skal ikke instansieres direkte
    
    private Object verdi;
    
    public static final Verdielement verdien(Object verdi) {
        Verdielement ve = new Verdielement();
        
        ve.verdi = generaliser(verdi);
        return ve;
    }
    
    public static final Verdielement argumentet(Object variabel) {
        return verdien(KontekstManager.hentVariabel(variabel));
    }

    @SuppressWarnings("unused")
    public static final Verdielement antallet(Iterable<?> settet) {
        int ant = 0;
        for (Object whatever : settet) {
            ant++;
        }
        return verdien(ant);
    }
    
    public Object verdi() {
        return verdi;
    }
    
    public Predikat mangler() {
        return () -> verdi == null;
    }

    public Predikat harVerdi() {
        return () -> verdi != null;
    }
    
    public Predikat erLik(Object annetVerdi) {
        return () -> harVerdiLik(annetVerdi); // Ja, vi vil gjerne ha teknisk feil / NPE hvis verdi er null
    }

    public Predikat erEnAv(Object... andreVerdier) {
        return () -> {
            for (Object annetVerdi : andreVerdier) {
                if (harVerdiLik(annetVerdi)) { // Ja, vi vil ha teknisk feil / NPE hvis verdi er null
                    return true;
                }
            }
            return false;
        };
    }
    
    public Predikat erSann() {
        return () -> {
            Assert.isTrue(verdi instanceof Boolean || Boolean.TYPE.isInstance(verdi), "Kun boolske verdier kan være sanne eller usanne");
            return harVerdiLik(true);
        };
    }

    public Predikat erIkkeSann() {
        return () -> {
            Assert.isTrue(verdi instanceof Boolean || Boolean.TYPE.isInstance(verdi), "Kun boolske verdier kan være sanne eller usanne");
            return harVerdiLik(false);
        };
    }

    public <T> Predikat erStørreEnn(T grense) {
        return () -> sammenliknMed(grense) > 0;
    }

    public Predikat erStørreEnnEllerLik(Object grense) {
        return () -> sammenliknMed(grense) >= 0;
    }
    
    public Predikat erMindreEnnEllerLik(Object grense) {
        return () -> sammenliknMed(grense) <= 0;
    }
    
    private boolean harVerdiLik(Object annetVerdi) {
        return verdi.equals(generaliser(annetVerdi));
    }

    @SuppressWarnings("unchecked")
    private int sammenliknMed(Object grense) {
        try {
            return ((Comparable<Object>) verdi).compareTo(generaliser(grense));
        } catch (ClassCastException e) {
            throw new RuntimeException("Kan ikke sammenligne en " + verdi.getClass() + " med en " + grense.getClass());
        }
    }

    /**
     * "Forfremmer" enkelte objekter (som f.eks. int til long).
     */
    private static Object generaliser(Object v) {
        if (v instanceof Integer) return ((Integer) v).longValue();
        return v;
    }
    
    @Override
    public String toString() {
        return verdi == null ? "null" : verdi.toString();
    }
    
}
