package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.leggTilLovvalgsbestemmelse;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.logg;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.regler.api.lovvalg.Artikkel;
import no.nav.melosys.regler.api.lovvalg.Lovvalgsbestemmelse;
import no.nav.melosys.regler.api.lovvalg.Resultat;
import no.nav.melosys.regler.nare.Betingelse;
import no.nav.melosys.regler.nare.ManuellBetingelse;
import no.nav.melosys.regler.nare.MaskinellBetingelse;
import no.nav.melosys.regler.nare.Predikat;

/**
 * Sjekker om konteksten tilfredsstiller en bestemt artikkel
 * 
 * FIXME (farjam): Legg til instrukser for hvordan man lager nye Regelsett
 */
public abstract class Regelsett {
    
    // Teknisk navn. Brukes kun for logging.
    private String tekniskNavn;
    
    private Artikkel artikkel;
    private List<Betingelse> betingelser = new ArrayList<>();
    

    /**
     * Setter artikkel/lovhjemmel for regelsettet
     */
    protected void forArtikkel(Artikkel artikkel) {
        this.artikkel = artikkel;
        // Sett teknisk navn på regelsettet med det saamme. Dette er klassenavnet til kaller.
        String regelKlasse = getClass().getCanonicalName();
        tekniskNavn = regelKlasse.replaceAll("no.nav.melosys.regler.lovvalg.", "");
    }
    
    /**
     * Legger til en maskinell betingelse på regelsettet.
     */
    protected Regelsett medMaskinellBetingelse(String beskrivelse, Predikat... predikater) {
        MaskinellBetingelse bet = new MaskinellBetingelse(beskrivelse, predikater);
        betingelser.add(bet);
        return this;
    }
    
    /**
     * Legger til en manuell betingelse på regelsettet.
     */
    protected Regelsett medManuellBetingelse(String beskrivelse) {
        ManuellBetingelse bet = new ManuellBetingelse(beskrivelse);
        betingelser.add(bet);
        return this;
    }
    
    /**
     * Kjører regelsettet og oppdaterer konteksten med rusltatet
     */
    public final void kjør() {
        logg(tekniskNavn, "Sjekker betingelsene for artikkel {}", artikkel);
        Lovvalgsbestemmelse bestemmelse = new Lovvalgsbestemmelse();
        bestemmelse.artikkel = artikkel;
        bestemmelse.betingelser = new ArrayList<>();
        for (Betingelse b : betingelser) {
            Resultat resultat = b.evaluer();
            logg(tekniskNavn, "{}: {}", b.getBeskrivelse(), resultat);
            no.nav.melosys.regler.api.lovvalg.Betingelse resBetingelse = new no.nav.melosys.regler.api.lovvalg.Betingelse();
            resBetingelse.beskrivelse = b.getBeskrivelse();
            resBetingelse.resultat = resultat;
            bestemmelse.betingelser.add(resBetingelse);
        }
        leggTilLovvalgsbestemmelse(bestemmelse);
    }

}
