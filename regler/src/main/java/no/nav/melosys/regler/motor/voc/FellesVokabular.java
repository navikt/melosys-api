package no.nav.melosys.regler.motor.voc;

import java.time.LocalDate;

public class FellesVokabular {

    public final static Boolean JA = Boolean.TRUE;
    public final static Boolean NEI = Boolean.FALSE;
    
    // Dato da forordning 883/2004 trer i kraft for EØS
    public final static LocalDate FØRSTE_JUNI_2012 = LocalDate.of(2012, 6, 1);

    // Dato da forordning 883/2004 gjelder også for Sveits
    public final static LocalDate FØRSTE_JANUAR_2016 = LocalDate.of(2016, 1, 1);

    // Dato da nordisk konvensjon om trygd trer i kraft
    public final static LocalDate FØRSTE_MAI_2014 = LocalDate.of(2014, 5, 1);

    /**
     * Utfører alle kommandoene i sekvens
     * OBS! Kommandoene utføres i sekvens. Men husk at alle kommandoene konstrueres FØR den første eksekveres.
     */
    public static final void utfør(Runnable... runnables) {
        for (Runnable r : runnables) {
            r.run();
        }
    }
}
