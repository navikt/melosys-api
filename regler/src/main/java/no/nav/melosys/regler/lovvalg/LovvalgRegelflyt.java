package no.nav.melosys.regler.lovvalg;

import no.nav.melosys.regler.lovvalg.utled_fakta.SjekkOmSoeknadenDekkesAvEf_883_2004;
import no.nav.melosys.regler.lovvalg.utled_fakta.UtledFaktaOmArbeid;
import no.nav.melosys.regler.lovvalg.utled_fakta.UtledFaktaOmPerson;
import no.nav.melosys.regler.lovvalg.utled_fakta.UtledFaktaOmUtenlandsopphold;
import no.nav.melosys.regler.lovvalg.velg_artikkel.KvalifiserForArtikler;
import no.nav.melosys.regler.lovvalg.verifiser_inndata.AvbrytRegelkjoeringHvisFeil;
import no.nav.melosys.regler.lovvalg.verifiser_inndata.ValiderInndata;
import no.nav.melosys.regler.lovvalg.verifiser_inndata.VerifiserPaakrevdeFelter;
import no.nav.melosys.regler.lovvalg.vurder_artikler.VurderArtikler;
import no.nav.melosys.regler.motor.Regelflyt;

/** Hovedregelflyt for lovvalgtjenesten */
public class LovvalgRegelflyt extends Regelflyt {
    
    private static final LovvalgRegelflyt instanse = new LovvalgRegelflyt();
    
    private LovvalgRegelflyt() {
        
        // Steg 1: Verifiser inndata
        leggTilRegelpakker(
            VerifiserPaakrevdeFelter.class,
            ValiderInndata.class,
            AvbrytRegelkjoeringHvisFeil.class
        );
        
        // Steg 2: Utled fakta
        leggTilRegelpakker(
            SjekkOmSoeknadenDekkesAvEf_883_2004.class,
            UtledFaktaOmPerson.class,
            UtledFaktaOmArbeid.class,
            UtledFaktaOmUtenlandsopphold.class
        );
        
        // Steg 3: Finn ut hvilke(t) artikler som er relevante
        leggTilRegelpakker(
            KvalifiserForArtikler.class
        );
        
        // Steg 4: Vurder om kravene til relevante artikler oppfylles
        leggTilRegelpakker(
            VurderArtikler.class
        );

        // Steg X: Fjern artikler som blir trumfet av andre artikler
        
    }
    
    public static LovvalgRegelflyt getInstanse() {
        return instanse;
    }

}
