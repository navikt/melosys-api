package no.nav.melosys.regler.nare;

import no.nav.melosys.regler.api.lovvalg.Resultat;

/**
 * Felles klasse for betingelser.
 */
public class MaskinellBetingelse extends Betingelse {
    
    private Predikat[] predikater;

    /** 
     * Oppretter en betingelse som skal vurderes maskinelt, med tilhørende kvalifiserende predikater.
     */
    public MaskinellBetingelse(String beskrivelse, Predikat... predikater) {
        super(beskrivelse);
        assert predikater != null;
        this.predikater = predikater;
    }
    
    @Override
    public Resultat evaluer() {
        for (Predikat predikat : predikater) {
            if (!predikat.test()) {
                return Resultat.IKKE_OPPFYLT;
            }
        }
        return Resultat.OPPFYLT;
    }
    
}
