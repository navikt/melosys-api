package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.responsen;
import static no.nav.melosys.regler.motor.Predikat.ikke;

import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.motor.Predikat;

public final class LovvalgPredikater {

    private LovvalgPredikater() {}
    
    /** Predikat som er sant hvis vi har fått en feilmelding. */
    public static Predikat detErMeldtFeil = () -> {
        for (Feilmelding feil : responsen().feilmeldinger) {
            if (feil.kategori.alvorlighetsgrad == Alvorlighetsgrad.FEIL) {
                return true;
            }
        }
        return false;
    };

    /** Predikat som er sant hvis vi ikke har fått en feilmelding. */
    public static final Predikat detErIkkeMeldtFeil = ikke(detErMeldtFeil);
    

    
    /*
    public static final Predikat personenErArbeidstaker = () -> {return søknad().arbeidstakerEllerSelvstendigNaeringsdrivende;}; 

    public static final Predikat personenErSelvstendigNæringsdrivende = () -> {return søknad().arbeidstakerOgSelvstendigNaeringsdrivende;}; 
    
    public static final Predikat personenArbeiderPåSkip = () -> {return søknad().arbeidSkip;};

    public static final Predikat personenArbeiderPåSokkel = () -> {return søknad().arbeidSokkel;};

    public static final Predikat antallMånederIPeriodenErMindreEnnEllerLik(int maxMåneder) {
        return  () -> {return søknad().periodeFom.plusMonths(maxMåneder).isAfter(søknad().periodeTom);}; 
    }

    public static final Predikat antallLandErMindreEnnEllerLik(int maxLand) {
        return  () -> {return søknad().land.size() <= maxLand;}; 
    }
    //*/

}
