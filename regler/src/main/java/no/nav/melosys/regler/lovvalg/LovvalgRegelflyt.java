package no.nav.melosys.regler.lovvalg;

import no.nav.melosys.regler.lovvalg.sett_variabler.SettVariabler;
import no.nav.melosys.regler.lovvalg.verifiser_inndata.AvbrytRegelkjoeringHvisFeil;
import no.nav.melosys.regler.lovvalg.verifiser_inndata.ValiderInndata;
import no.nav.melosys.regler.lovvalg.verifiser_inndata.VerifiserPaakrevdeFelter;
import no.nav.melosys.regler.motor.Regelflyt;

public class LovvalgRegelflyt extends Regelflyt {
    
    public LovvalgRegelflyt() {
        
        // Steg 1: Verifiser inndata
        leggTilRegelpakke(VerifiserPaakrevdeFelter.class);
        leggTilRegelpakke(ValiderInndata.class);
        leggTilRegelpakke(AvbrytRegelkjoeringHvisFeil.class);
        
        // Steg 2: Sett verdier
        leggTilRegelpakke(SettVariabler.class);
        
        // Steg X: Finn ut hvilke(t) artikler som er relevante
        
        // Steg X: Sjekk hvilken artikkel som slår inn
    }

}
