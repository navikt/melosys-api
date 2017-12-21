package no.nav.melosys.regler.lovvalg.vurder_artikler;

import no.nav.melosys.regler.motor.Regelflyt;
import no.nav.melosys.regler.motor.Regelpakke;

/**
 * Sub-flyt for vurdering av allartikler
 */
public class VurderArtikler extends Regelflyt implements Regelpakke {
    
    private VurderArtikler() {
        leggTilRegelpakker(
            VurderArtikkel12_1.class
        );
    }

    private static VurderArtikler instanse = new VurderArtikler();
    
    @Regel
    public static void kjørSubflyt() {
        instanse.kjør();
    }
    
}
