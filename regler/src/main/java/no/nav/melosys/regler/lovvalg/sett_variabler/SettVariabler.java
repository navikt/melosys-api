package no.nav.melosys.regler.lovvalg.sett_variabler;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.BRUKER_ER_ARBEIDSTAKER;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.DELVIS_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgImparater.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgImparater.settVariabel;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.arbeidsforholdDokumentene;
import static no.nav.melosys.regler.lovvalg.LovvalgPredikater.harOverlappMedSøknadsperioden;
import static no.nav.melosys.regler.lovvalg.LovvalgProdusenter.arbeidsforholdene;
import static no.nav.melosys.regler.lovvalg.LovvalgProdusenter.permitteringer;
import static no.nav.melosys.regler.motor.dekl.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.dekl.Verdielement.antallet;

import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.PermisjonOgPermittering;
import no.nav.melosys.regler.api.lovvalg.rep.Kategori;
import no.nav.melosys.regler.motor.KontekstManager;
import no.nav.melosys.regler.motor.Regel;
import no.nav.melosys.regler.motor.Regelpakke;

public class SettVariabler extends Regelpakke { // Utled fakta
    
    /**
     * Gir et varsel dersom bruker har en permittering som overlapper med søknadsperioden.
     */
    @Regel
    public static void giVarselHvisPermitteringISøknadsperioden_IMPERATIVT() {
        for (ArbeidsforholdDokument arbeidsforholdDokument : arbeidsforholdDokumentene()) {
            for (Arbeidsforhold arbeidsforhold : arbeidsforholdDokument.getArbeidsforhold()) {
                for (PermisjonOgPermittering permittering : arbeidsforhold.getPermisjonOgPermittering()) {
                    if (harOverlappMedSøknadsperioden.test(permittering)) {
                        leggTilMelding(Kategori.DELVIS_STOETTET, "Bruker har en eller flere permitteringer som overlapper med søknadsperioden"); // Mangler run()
                        return; // Holder å gi varsel én gang per forespørsel
                    }
                }
            }
        }
    }   
    
    /**
     * Gir et varsel dersom bruker har en permittering som overlapper med søknadsperioden.
     */
    @Regel
    public static void giVarselHvisPermitteringISøknadsperioden_STREAM() {
        if (arbeidsforholdDokumentene().stream()
            .flatMap(ad -> {return ad.getArbeidsforhold().stream();})
            .flatMap(a -> {return a.getPermisjonOgPermittering().stream();})
            .anyMatch(harOverlappMedSøknadsperioden)) {
            leggTilMelding(Kategori.DELVIS_STOETTET, "Bruker har en eller flere permitteringer som overlapper med søknadsperioden"); // Mangler run()
        }
    }
    
    /**
     * Gir et varsel dersom bruker har en permittering som overlapper med søknadsperioden.
     */
    @Regel
    public static void giVarselHvisPermitteringISøknadsperioden_DEKLARATIVT() {
        hvis(antallet(arbeidsforholdene().sine(permitteringer).som(harOverlappMedSøknadsperioden))
            .erStørreEnnEllerLik(1))
        .så(leggTilMelding(DELVIS_STOETTET, "Bruker har en eller flere permitteringer som overlapper med søknadsperioden"));
    }
    
    
    
    
    
    /**
     * Bestemmer om bruker er arbeidstaker eller ikke.
     * Setter variabelen BRUKER_ER_ARBEIDSTAKER
     * 
     * En bruker er en arbeidstaker dersom vedkommede har et arbeidsforhold som dekker hele søknadsperioden, og hvor det ikke finnes noen permisjon i søknadsperioden
     */
    @Regel
    public static void finnUtOmBrukerErArbeidstaker_IMPERATIVT() {
        for (ArbeidsforholdDokument arbeidsforholdDokument : arbeidsforholdDokumentene()) {
            for (Arbeidsforhold arbeidsforhold : arbeidsforholdDokument.getArbeidsforhold()) {
                if (harOverlappMedSøknadsperioden.test(arbeidsforhold)) {
                    KontekstManager.settVariabel(BRUKER_ER_ARBEIDSTAKER, true);
                    return;
                }
            }
        }
    }

    /**
     * Bestemmer om bruker er arbeidstaker eller ikke.
     * Setter variabelen BRUKER_ER_ARBEIDSTAKER
     */
    @Regel
    public static void finnUtOmBrukerErArbeidstaker_DEKLARATIVT() {
        hvis(antallet(arbeidsforholdene().som(harOverlappMedSøknadsperioden))
            .erStørreEnnEllerLik(1))
        .så(settVariabel(BRUKER_ER_ARBEIDSTAKER, true));
    }

    /* FIXME: regler for å sette disse argumentene:
    BRUKER_ER_NÆRINGSDRIVENDE("Bruker er næringsdrivende"),
    BRUKER_ER_TJENESTEMANN("Bruker er tjenestemann"),
    BRUKER_ARBEIDER_I_FLY("Bruker arbeider i fly"),
    BRUKER_ARBEIDER_PÅ_SKIP("Bruker arbeider på skip"),
    BRUKER_ARBEIDER_I_FLERE_LAND("Bruker arbeider i flere land"),
    ANDEL_PROSENT_ARB_I_BOLAND("Andel % arb/inntekt i bostedsland"),
    */

}
