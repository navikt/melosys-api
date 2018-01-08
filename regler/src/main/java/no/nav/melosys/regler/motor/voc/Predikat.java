package no.nav.melosys.regler.motor.voc;

/**
 * Et predikat som tester konteksten. 
 */
public interface Predikat {

    /**
     * Klassens eneste abstrakte metode (slik at vi kan angi predikater med lambdauttrykk).
     */
    public boolean test();
    
    /**
     * Returnerer et nytt predikat som er det omvendte/inverse av dette predikatet.
     */
    public static Predikat ikke(Predikat p) {
        return () -> !p.test();
    }

    /**
     * Returnerer et nytt predikat som er konjunksjonen av dette og et annet prediakt.
     */
    public default Predikat og(Predikat p) {
        return () -> this.test() && p.test();
    }

    /**
     * Returnerer ett nytt predikat som er disjunksjonen av dette og et annet predikat.
     */
    public default Predikat eller(Predikat p) {
        return () -> this.test() || p.test();
    }
    
    public static Predikat minstEttAvFølgendeErSant(Predikat... predikater) {
        return () -> {
            for (Predikat p : predikater) {
                if (p.test()) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Predikat ingenAvFølgendeErSant(Predikat... predikater) {
        return () -> {
            for (Predikat p : predikater) {
                if (p.test()) {
                    return false;
                }
            }
            return true;
        };
    }

}
