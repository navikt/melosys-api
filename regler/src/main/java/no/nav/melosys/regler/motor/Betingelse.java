package no.nav.melosys.regler.motor;

import net.bytebuddy.asm.Advice.Argument;
import no.nav.melosys.regler.api.lovvalg.rep.Resultat;

/**
 * Felles klasse for betingelser.
 */
public class Betingelse {
    
    private Argument argument;
    private Predikat[] predikater;

    /** 
     * Oppretter en betingelse som skal vurderes maskinelt, med tilhørende kvalifiserende predikater.
     */
    public Betingelse(Argument beskrivelse, Predikat... predikater) {
        this.argument = beskrivelse;
        this.predikater = predikater;
    }
    
    /**
     * Funksjonell beskrivelse av betingelsen.
     */
    public Argument getBeskrivelse() {
        return argument;
    }

    public Resultat evaluer() {
        for (Predikat predikat : predikater) {
            if (!predikat.test()) {
                return Resultat.IKKE_OPPFYLT;
            }
        }
        return Resultat.OPPFYLT;
    }
    
}
