package no.nav.melosys.regler.lovvalg.utsendt_arbeidstaker;

import static no.nav.melosys.regler.api.lovvalg.Artikkel.ART_12_1;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.antallMånederIPeriodenErMindreEnnEllerLik;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.personenErArbeidstaker;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.*;
import static no.nav.melosys.regler.nare.Predikat.ikke;

import no.nav.melosys.regler.lovvalg.Regelsett;

public class Art_12_1_UtsendtArbeidstaker extends Regelsett {
    
    public Art_12_1_UtsendtArbeidstaker() {
        forArtikkel(ART_12_1);
        medMaskinellBetingelse("Personen må være arbeidstaker", personenErArbeidstaker);
        medMaskinellBetingelse("Personen må ikke være selvstendig næringsdrivende", ikke(personenErSelvstendigNæringsdrivende));
        medMaskinellBetingelse("Skal ikke gjelde arbeid på skip", ikke(personenArbeiderPåSkip));
        medMaskinellBetingelse("Skal ikke gjelde arbeid på sokkel", ikke(personenArbeiderPåSokkel));
        medManuellBetingelse("Arbeidsgiver driver vanligvis virksomhet i landet arbeidstakeren sendes ut fra");
        medMaskinellBetingelse("Personen skal sendes til kun ett land", antallLandErMindreEnnEllerLik(1));
        medManuellBetingelse("Arbeidstakeren sendes til en annen medlemsstat for å utføre arbeid for arbeidsgiveren");
        medMaskinellBetingelse("Oppholdet skal ikke vare lengere enn 24 måneder", antallMånederIPeriodenErMindreEnnEllerLik(24));
        medManuellBetingelse("Arbeidstakeren er ikke utsendt for å erstatte en annen person");

        // FIXME (farjam): Legg till alle betingelsene (forsvaret / internasjonal transport / ...)
    }

}

