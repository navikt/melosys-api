package no.nav.melosys.regler.lovvalg;

import java.util.function.Predicate;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periodetype;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.motor.voc.Predikat;

import static no.nav.melosys.domain.dokument.felles.Land.NORGE;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.responsen;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknadDokumentet;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknadsperioden;
import static no.nav.melosys.regler.motor.voc.Verdielement.verdien;

/**
 * Verbalisering av predikater
 */
public final class LovvalgPredikater {

    private LovvalgPredikater() {}
    
    /** Predikat som er sant hvis vi har fått en feilmelding. */
    public static final Predikat detErMeldtFeil = () -> {
        for (Feilmelding feil : responsen().feilmeldinger) {
            if (feil.kategori.alvorlighetsgrad == Alvorlighetsgrad.FEIL) {
                return true;
            }
        }
        return false;
    };

    /** Sjekker om et element sin periode dekker (hele) søknadens periode */
    public static final Predicate<HarPeriode> dekkerHeleSøknadsperioden = e -> {
        ErPeriode p = e.getPeriode();
        // Teknisk feil hvis p er null. OK med NPE.
        ErPeriode søknadsperiode = søknadDokumentet().oppholdUtland.getPeriode();
        if (p.getFom() != null && p.getFom().isAfter(søknadsperiode.getFom())) {
            // Elementets fom er etter søknadens fom
            return false;
        }
        if (p.getTom() != null && p.getTom().isBefore(søknadsperiode.getTom())) {
            // Elementets tom er før søknadens tom
            return false;
        }
        return true;
    };
    
    /** Sjekker om et element sin periode har overlapp med søknadens periode */
    public static final Predicate<HarPeriode> harOverlappMedSøknadsperioden = e -> {
        ErPeriode p = e.getPeriode();
        // Teknisk feil hvis p er null. OK med NPE.
        ErPeriode søknadsperiode = søknadDokumentet().oppholdUtland.getPeriode();
        if (p.getFom() != null && p.getFom().isAfter(søknadsperiode.getTom())) {
            // Elementets fom er etter søknadens tom
            return false;
        }
        if (p.getTom() != null && p.getTom().isBefore(søknadsperiode.getFom())) {
            // Elementets tom er før søknadens fom
            return false;
        }
        return true;
    };

    /** Sjekker om opptjeningsland er i utlandet */
    public static final Predicate<Inntekt> inntektOpptjentIUtlandet = (Inntekt inntekt)
        -> !inntekt.opptjeningsland.equals(NORGE);

    /** Sjekker om bostedsadresse er i Norge */
    public static final Predicate<Bostedsadresse> bostedsadresseErINorge = (Bostedsadresse b)
        -> b.getLand().getKode().equals(NORGE);

    /** Sjekker om brukeren arbeidet i Norge før periodestart */
    public static final Predicate<Arbeidsforhold> ansattINorgeFørPeriodestart = (Arbeidsforhold arbeidsforhold)
        -> søknadsperioden()
            .starterPåEllerEtter(verdien(arbeidsforhold).startdato())
            .og(verdien(arbeidsforhold.getUtenlandsopphold()).mangler()).test();

    public static final Predicate<Medlemsperiode> medlemAvFtrlFørPeriodestart = (Medlemsperiode medlemsperiode)
        // FIXME: Skal sjekke om medlemsperioden var måneden før utenlandsopphold
        -> søknadsperioden()
            .starterPåEllerEtter(verdien(medlemsperiode).startdato())
            .og(verdien(medlemsperiode.type).oppfyller(type -> type.equals(Periodetype.PMMEDSKP))).test();

}
