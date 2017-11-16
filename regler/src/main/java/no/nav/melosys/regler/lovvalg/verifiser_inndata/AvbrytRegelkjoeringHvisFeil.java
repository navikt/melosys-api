package no.nav.melosys.regler.lovvalg.verifiser_inndata;

import static no.nav.melosys.regler.lovvalg.LovvalgImparater.avbrytRegelkjøring;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.detErMeldtFeil;
import static no.nav.melosys.regler.motor.dekl.Deklarasjon.hvis;

import no.nav.melosys.regler.motor.Regel;
import no.nav.melosys.regler.motor.Regelpakke;

public class AvbrytRegelkjoeringHvisFeil extends Regelpakke {
    
    @Regel
    public static void avbrytRegelkjøringHvisFeil() {
        hvis(detErMeldtFeil).så(avbrytRegelkjøring);
    }

}
