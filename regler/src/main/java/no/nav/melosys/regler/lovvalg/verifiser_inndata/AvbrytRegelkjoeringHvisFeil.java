package no.nav.melosys.regler.lovvalg.verifiser_inndata;

import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.avbrytRegelkjøring;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.detErMeldtFeil;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;

import no.nav.melosys.regler.motor.Regelpakke;

public class AvbrytRegelkjoeringHvisFeil implements Regelpakke {
    
    @Regel
    public static void avbrytRegelkjøringHvisFeil() {
        hvis(detErMeldtFeil).så(avbrytRegelkjøring);
    }

}
