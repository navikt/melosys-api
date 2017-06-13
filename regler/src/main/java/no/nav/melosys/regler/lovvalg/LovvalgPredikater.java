package no.nav.melosys.regler.lovvalg;


import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.søknad;

import no.nav.melosys.regler.nare.Predikat;

public final class LovvalgPredikater {
    
    private LovvalgPredikater() {}
    
    public static final Predikat personenErArbeidstaker = () -> {return søknad().arbeidstakerEllerSelvstendigNaeringsdrivende;}; 

    public static final Predikat personenErSelvstendigNæringasdrivende = () -> {return søknad().arbeidstakerOgSelvstendigNaeringsdrivende;}; 

    public static final Predikat antallMånederIPeriodenErMindreEnnEllerLik(int maxMåneder) {
        return  () -> {return søknad().periodeFom.plusMonths(maxMåneder).isAfter(søknad().periodeTom);}; 
    }

}
