package no.nav.melosys.regler.motor.voc;

public class FellesVokabular {

    public final static Boolean JA = Boolean.TRUE;
    public final static Boolean NEI = Boolean.FALSE;
    
    /**
     * Utfører alle kommandoene i sekvens
     */
    public static final void utfør(Runnable... runnables) {
        for (Runnable r : runnables) {
            r.run();
        }
    }
}
