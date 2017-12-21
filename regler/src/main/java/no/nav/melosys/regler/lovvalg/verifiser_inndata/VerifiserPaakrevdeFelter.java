package no.nav.melosys.regler.lovvalg.verifiser_inndata;

import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.VALIDERINGSFEIL;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMeldingOgAvbryt;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.*;
import static no.nav.melosys.regler.lovvalg.LovvalgProdusenter.arbeidsforholdene;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.voc.Verdielement.verdien;
import static no.nav.melosys.regler.motor.voc.VerdielementSett.forAlle;

import no.nav.melosys.regler.motor.Regelpakke;

public class VerifiserPaakrevdeFelter implements Regelpakke {
    
    /** Verifiserer at vi har en søknaden, og at den inneholder alle felter som er på påkrevd. */
    @Regel
    public static void sjekkSøknaden() {
        // Sjekk at vi har en søknad
        hvis(verdien(søknadDokumentet()).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Forespørselen mangler søknad"));

        // Sjekk periode
        hvis(verdien(søknadDokumentet().arbeidUtland.arbeidsperiode).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Søknaden mangler periode"));
        hvis(verdien(søknadDokumentet().arbeidUtland.arbeidsperiode.getFom()).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Søknaden mangler fomDato"));
        hvis(verdien(søknadDokumentet().arbeidUtland.arbeidsperiode.getTom()).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Søknaden mangler tomDato"));

        /* FIXME: Sjekk alle felter som er påkrevd for alle søknader
            // Sjekk flaggland...
            if (søknad().arbeidSkip && søknad().skipFlaggland == null) {
                leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Arbeid på skip, men ikke oppgitt flaggland");
            }

            // Sjekk sokkel-land
            if (søknad().arbeidSokkel && søknad().sokkelLand == null) {
                leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Arbeid på sokkel, men ikke oppgitt sokkelland");
            }

            // Sjekk land
            if (søknad().land == null) { // Kun for convenience for å slippe å sjekke for null i andre tester
                søknad().land = Collections.emptyList();
            }
            if (søknad().land.isEmpty()) {
                leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Ingen land oppgitt i søknaden");
            }
        //*/
    }
    
    /** Verifiserer at alle arbeidsforhold har alle feltene som er påkrevd. */
    @Regel
    public static void sjekkAlleArbeidsfolhold() {
        forAlle(arbeidsforholdene())
        .utfør(arbeidsforholdet -> {
            hvis(verdien(arbeidsforholdet.getPeriode()).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Det er minst ett arbeidsforhold som mangler periode"));
            // FIXME: Ytterligere nullsjekk
        });
    }
    
    @Regel
    public static void sjekkPersonopplysninger() {
        hvis(verdien(personopplysningDokumentet()).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Forespørselen mangler personopplysninger"));

        // Sjekk fnr
        hvis(verdien(personopplysningDokumentet().fnr).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Personopplysningene mangler fnr"));

        // Sjekk statsborgerskap
        hvis(verdien(personopplysningDokumentet().statsborgerskap).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Personopplysningene mangler statsborgerskap"));

        // Sjekk fødselsdato
        hvis(verdien(personopplysningDokumentet().fødselsdato).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Personopplysningene mangler fødselsdato"));

        // Sjekk personstatus
        hvis(verdien(personopplysningDokumentet().personstatus).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Personopplysningene mangler personstatus"));
    }
    
    @Regel
    public static void sjekkInntekt() {
        forAlle(inntektDokumentene())
        // .som(relevant periode) FIXME 
        .utfør(inntektDokument -> {
            // FIXME
        });
    }
    
    @Regel
    public static void sjekkMedlemskap() {
        forAlle(medlemskapDokumentene())
        // .som(relevant periode) FIXME 
        .utfør(medlemskapDokument -> {
            // FIXME
        });
    }

    @Regel
    public static void sjekkOrganisasjon() {
        forAlle(organisasjonDokumentene())
        // .som(relevant periode) FIXME 
        .utfør(organisasjonDokument -> {
            // FIXME
        });
    }

}
