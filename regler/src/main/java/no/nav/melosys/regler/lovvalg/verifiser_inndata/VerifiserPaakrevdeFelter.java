package no.nav.melosys.regler.lovvalg.verifiser_inndata;

import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.VALIDERINGSFEIL;
import static no.nav.melosys.regler.lovvalg.LovvalgImparater.leggTilMeldingOgAvbryt;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.inntektDokumentene;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.medlemskapDokumentene;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.organisasjonDokumentene;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.personopplysningDokumentet;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknadDokumentet;
import static no.nav.melosys.regler.lovvalg.LovvalgProdusenter.arbeidsforholdene;
import static no.nav.melosys.regler.motor.dekl.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.dekl.Verdielement.verdien;
import static no.nav.melosys.regler.motor.dekl.VerdielementSett.forAlle;

import no.nav.melosys.regler.motor.Regel;
import no.nav.melosys.regler.motor.Regelpakke;

public class VerifiserPaakrevdeFelter extends Regelpakke {
    
    /*
     * 
     * FIXME: Her kan vi vurdere å kun gjøre dette "verbalisert" der det er funksjonelt relevant (noe det stort sett ikke er).
     * Mao. kan denne funksjonaliteten godt skrives som vanlig java-kode.
     * 
     */

    /** Verifiserer at vi har en søknaden, og at den inneholder alle felter som er på påkrevd. */
    @Regel
    public static void sjekkSøknaden() {
        // Sjekk at vi har en søknad
        hvis(verdien(søknadDokumentet()).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Forespørselen mangler søknad"));

        // Sjekk periode
        hvis(verdien(søknadDokumentet().periode).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Søknaden mangler periode"));
        hvis(verdien(søknadDokumentet().periode.getFom()).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Søknaden mangler fomDato"));
        hvis(verdien(søknadDokumentet().periode.getTom()).mangler()).så(leggTilMeldingOgAvbryt(VALIDERINGSFEIL, "Søknaden mangler tomDato"));

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
        // .hvor(relevant periode) FIXME 
        .utfør(inntektDokument -> {
            // FIXME
        });
    }
    
    @Regel
    public static void sjekkMedlemskap() {
        forAlle(medlemskapDokumentene())
        // .hvor(relevant periode) FIXME 
        .utfør(medlemskapDokument -> {
            // FIXME
        });
    }

    @Regel
    public static void sjekkOrganisasjon() {
        forAlle(organisasjonDokumentene())
        // .hvor(relevant periode) FIXME 
        .utfør(organisasjonDokument -> {
            // FIXME
        });
    }

}
