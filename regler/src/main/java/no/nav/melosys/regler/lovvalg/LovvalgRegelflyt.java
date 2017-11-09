package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.*;
import static no.nav.melosys.regler.motor.Predikat.*;

import no.nav.melosys.regler.lovvalg.verifiser_inndata.ValiderInndata;
import no.nav.melosys.regler.lovvalg.verifiser_inndata.VerifiserPaakrevdeElementer;
import no.nav.melosys.regler.lovvalg.verifiser_inndata.VerifiserPaakrevdeFelter;
import no.nav.melosys.regler.motor.AvbrytRegelkjoeringIStillhet;
import no.nav.melosys.regler.motor.Predikat;
import no.nav.melosys.regler.motor.Regelflyt;

public class LovvalgRegelflyt extends Regelflyt {
    
    public LovvalgRegelflyt() {
        
        // Steg 1: Verifiser inndata
        leggTilRegelpakke(VerifiserPaakrevdeElementer.class);
        leggTilRegelpakke(detErIkkeMeldtFeil, VerifiserPaakrevdeFelter.class);
        leggTilRegelpakke(detErIkkeMeldtFeil, ValiderInndata.class);

        // Avbryt regelflyten hvis det flagges feil
        leggTilRegelpakke(detErMeldtFeil, AvbrytRegelkjoeringIStillhet.class);
        
        // Steg 2: Sett verdier
        
        // Steg X: Finn ut hvilke(t) artikler som er relevante
        
        // Steg X: Sjekk hvilken artikkel som slår inn
    }

}
