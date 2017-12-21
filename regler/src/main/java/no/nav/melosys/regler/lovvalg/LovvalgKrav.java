package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.api.lovvalg.rep.Resultat.IKKE_OPPFYLT;
import static no.nav.melosys.regler.api.lovvalg.rep.Resultat.OPPFYLT;
import static no.nav.melosys.regler.motor.voc.Verdielement.argumentet;

import java.util.function.Function;

import no.nav.melosys.regler.api.lovvalg.rep.Argument;
import no.nav.melosys.regler.api.lovvalg.rep.Betingelse;
import no.nav.melosys.regler.motor.voc.Predikat;
import no.nav.melosys.regler.motor.voc.Verdielement;

/**
 * Krav relatert til argumenter0
 */
public class LovvalgKrav {
    
    /** Kravet som tekst */
    private String tekst;
    
    /** Funksjon som gir predikatet til kravet */
    private Function<Verdielement, Predikat> predikatFunksjon;
    
    private LovvalgKrav(String tekst, Function<Verdielement, Predikat> predikatFunksjon) {
        this.tekst = tekst;
        this.predikatFunksjon = predikatFunksjon;
    }

    public Function<Verdielement, Predikat> getPredikatFunksjon() {
        return predikatFunksjon;
    }

    public String getKravTekst() {
        return tekst;
    }
    
    /**
     * Evaluerer om et argument tilfredsstiller et kra v og lager og returnerer en Bewtingelse som viser evalueringen.
     * @param argument Argumentet som vurderes
     * @param krav Kravet argumentet skal oppfylle
     */
    public static final Betingelse betingelse(Argument argument, LovvalgKrav krav) {
        Betingelse bet = new Betingelse();
        bet.argument = argument;
        bet.krav = krav.getKravTekst();
        bet.resultat = krav.getPredikatFunksjon().apply(argumentet(argument)).test() ? OPPFYLT : IKKE_OPPFYLT;
        return bet;
    }

    /** Krav: Argumentet er satt til sant */
    public static final LovvalgKrav erSann = new LovvalgKrav("er sann", Verdielement::erSann);
    
    /** Krav: Argumentet er satt til sant */
    public static final LovvalgKrav erIkkeSann = new LovvalgKrav("er ikke sann", Verdielement::erIkkeSann);
    
    /** Krav: Argumentet er mindre enn eller lik */
    public static final LovvalgKrav erMindreEnnEllerLik(Object grense) {
        return new LovvalgKrav(
            "er mindre enn eller lik " + grense,
            v -> {return v.erMindreEnnEllerLik(grense);}
        );
    }

    /** Krav: Argumentet er lik */
    public static final LovvalgKrav erLik(Object krav) {
        return new LovvalgKrav(
            "er lik " + krav,
            v -> {return v.erLik(krav);}
        );
    }

}
