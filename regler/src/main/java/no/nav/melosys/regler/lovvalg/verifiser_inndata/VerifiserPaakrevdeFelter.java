package no.nav.melosys.regler.lovvalg.verifiser_inndata;

import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.VALIDERINGSFEIL;
import static no.nav.melosys.regler.lovvalg.LovvalgImparater.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.arbeidsforholdDokumentene;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.inntektDokumentene;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.medlemskapDokumentene;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.organisasjonDokumentene;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.personopplysningDokumentet;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknadDokumentet;
import static no.nav.melosys.regler.motor.dekl.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.dekl.VerdielementSamling.forAlle;

import no.nav.melosys.regler.motor.Regel;
import no.nav.melosys.regler.motor.Regelpakke;

public class VerifiserPaakrevdeFelter extends Regelpakke {

    // FIXME: Ikke ferdig
    
    @Regel
    public static void sjekkISøknaden() {
        utfør(
            // Sjekk periode
            hvis(søknadDokumentet().periode).harVerdi()
            .så(
                hvis(søknadDokumentet().periode.getFom()).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Søknaden mangler fomDato")),
                hvis(søknadDokumentet().periode.getTom()).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Søknaden mangler tomDato")))
            .ellers(leggTilMelding(VALIDERINGSFEIL, "Søknaden mangler periode"))
        );

        /*
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
    
    @Regel
    public static void sjekkArbeidsfolhold() {
        forAlle(arbeidsforholdDokumentene())
        // .hvor(relevant periode) FIXME 
        .utfør(arbeidsforholdDokument -> {
            // FIXME
        });
    }

    @Regel
    public static void sjekkPersonopplysninger() {
        utfør(
            // Sjekk fnr
            hvis(personopplysningDokumentet().fnr).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Personopplysningene mangler fnr")),

            // Sjekk sivilstand
            hvis(personopplysningDokumentet().sivilstand).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Personopplysningene mangler sivilstand")),
            
            // Sjekk statsborgerskap
            hvis(personopplysningDokumentet().statsborgerskap).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Personopplysningene mangler statsborgerskap")),

            // Sjekk fødselsdato
            hvis(personopplysningDokumentet().fødselsdato).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Personopplysningene mangler fødselsdato")),

            // Sjekk personstatus
            hvis(personopplysningDokumentet().personstatus).mangler().så(leggTilMelding(VALIDERINGSFEIL, "Personopplysningene mangler personstatus"))
        );
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
