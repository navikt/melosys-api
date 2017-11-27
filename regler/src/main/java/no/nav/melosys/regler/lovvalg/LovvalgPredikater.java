package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.responsen;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.søknadDokumentet;

import java.util.function.Predicate;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.HarPeriode;
import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.motor.dekl.Predikat;

/**
 * Klassen inneholder verbalisering av predikater
 */
public final class LovvalgPredikater {

    private LovvalgPredikater() {}
    
    /** Predikat som er sant hvis vi har fått en feilmelding. */
    public static Predikat detErMeldtFeil = () -> {
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
        ErPeriode søknadsperiode = søknadDokumentet().getPeriode();
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
        ErPeriode søknadsperiode = søknadDokumentet().getPeriode();
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
    
}
