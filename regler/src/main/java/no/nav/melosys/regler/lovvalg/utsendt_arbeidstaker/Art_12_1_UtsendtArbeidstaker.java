package no.nav.melosys.regler.lovvalg.utsendt_arbeidstaker;

import static no.nav.melosys.regler.api.lovvalg.Artikkel.ART_12_1;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.antallMånederIPeriodenErMindreEnnEllerLik;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.personenErArbeidstaker;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.personenErSelvstendigNæringasdrivende;
import static no.nav.melosys.regler.nare.Predikat.ikke;

import no.nav.melosys.regler.lovvalg.Regelsett;

public class Art_12_1_UtsendtArbeidstaker extends Regelsett {
    
    public Art_12_1_UtsendtArbeidstaker() {
        forArtikkel(ART_12_1);
        medMaskinellBetingelse("Personen må være arbeidstaker", personenErArbeidstaker);
        medMaskinellBetingelse("Personen må ikke være selvstendig næringsdrivende", ikke(personenErSelvstendigNæringasdrivende));
        medManuellBetingelse("Arbeidsgiver driver vanligvis virksomhet i landet arbeidstakeren sendes ut fra");
        medManuellBetingelse("Arbeidstakeren sendes til en annen medlemsstat for å utføre arbeid for arbeidsgiveren");
        medMaskinellBetingelse("Oppholdet skal ikke vare lengere enn 24 måneder", antallMånederIPeriodenErMindreEnnEllerLik(24));
        medManuellBetingelse("Arbeidstakeren er ikke utsendt for å erstatte en annen person");
    }
    
}
