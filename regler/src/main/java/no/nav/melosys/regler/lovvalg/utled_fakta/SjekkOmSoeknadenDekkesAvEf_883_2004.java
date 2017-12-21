package no.nav.melosys.regler.lovvalg.utled_fakta;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.SØKNADEN_KVALIFISERER_FOR_EF_883_2004;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.DELVIS_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.settArgument;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.voc.FellesVokabular.JA;

import no.nav.melosys.regler.motor.Regelpakke;
import no.nav.melosys.regler.motor.voc.Predikat;

/*'
 * Regelpakken slår fast om søknaden dekkes aV forordning (EF) 883/2004
 * 
 * FIXME: Ikke ferdig implementert. Pakken gir varsel dersom den ikke kan slå fast at søknaden dekkes. 
 */
public class SjekkOmSoeknadenDekkesAvEf_883_2004 implements Regelpakke {
    
    /**
     * Setter argumentet SØKNADEN_KVALIFISERER_FOR_EF_883_2004.
     * 
     * Tilfellet i søknaden er dekket av forordningen dersom ett av følgende slår til:
     * 
     * 1) Brukeren er statsborger av et EU/EØS-land
     * 2) Brukeren er statsborger av Sveits (etter en viss dato)
     * 3) Bruker er statsløs
     * 4) Bruker er flyktning
     * 5) Tilfellet dekkes av nordisk konvensjon om trygd
     *    Dvs. tredjelandsborger som sendes til Sverige, Finland, Danmark, Island, Grønland eller Færøyene
     * 6) Tilfellet dekkes av trygdeavtalen med Nederland
     *    Dvs. tredjelandsborger som sendes til Nederland
     * 7) Tilfellet dekkes av trygdeavtalen med Luxemburg
     *    Dvs. tredjelandsborger som sendes til Luxemburg
     */
    @Regel
    public static void sjekkOmSoeknadenDekkesAvEf_883_2004() {
        hvis(brukerErEøsBorger // Tilfelle 1
            // .eller(...)
            // FIXME: Legg til alle tilfellene som dekkes
        ).så(
            settArgument(SØKNADEN_KVALIFISERER_FOR_EF_883_2004, JA)
        ).ellers(
            // FIXME: Sett argumentet til NEI og gi feilmelding hvis alle tilfellene gjenkjennes
            settArgument(SØKNADEN_KVALIFISERER_FOR_EF_883_2004, JA),
            leggTilMelding(DELVIS_STOETTET, "Kan ikke fastslå om søknaden dekkes av forordning 883/2004. Må sjekkes manuelt.")
        );
    }
    
    private static Predikat brukerErEøsBorger = () -> {
        // FIXME: Implementer
        return false;
    };
    
}
