package no.nav.melosys.regler.lovvalg;


import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknad;

import no.nav.melosys.regler.nare.Predikat;

public final class LovvalgPredikater {
    
    private LovvalgPredikater() {}
    
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

}
