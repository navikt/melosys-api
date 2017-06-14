package no.nav.melosys.regler.nare;

import no.nav.melosys.regler.api.lovvalg.Resultat;

/**
 * En betingelse som ikke vurderes av regelmodulen (og som derfor må vurderes manuelt).
 */
public class ManuellBetingelse extends Betingelse {

    /** 
     * Oppretter en betingelse som ikke skal vurderes maskinelt.
     */
    public ManuellBetingelse(String beskrivelse) {
        super(beskrivelse);
    }

    @Override
    public Resultat evaluer() {
        return Resultat.VURDERES_MANUELT;
    }

}
