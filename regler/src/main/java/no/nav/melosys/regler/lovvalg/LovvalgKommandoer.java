package no.nav.melosys.regler.lovvalg;

import java.util.ArrayList;

import no.nav.melosys.regler.api.lovvalg.rep.*;
import no.nav.melosys.regler.motor.AvbrytRegelkjoeringIStillhetException;
import no.nav.melosys.regler.motor.KontekstManager;
import no.nav.melosys.regler.motor.voc.Verdielement;

import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.TEKNISK_FEIL;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.responsen;
import static no.nav.melosys.regler.motor.RegelLogg.loggError;
import static no.nav.melosys.regler.motor.RegelLogg.loggInfo;

/**
 * Klassen inneholder verbalisering av kommandoer
 * 
 * Alle metodene i denne klassen skriver til regellogg.
 * 
 */
public final class LovvalgKommandoer {

    private LovvalgKommandoer() {}

    /** Legger til en feil eller varsel på responsen som skal returnerer. */
    public static final Runnable leggTilMelding(Kategori kat, String melding) {
        return () -> {
            Feilmelding feil = new Feilmelding();
            feil.kategori = kat;
            feil.melding = melding;
            responsen().feilmeldinger.add(feil);
            loggInfo("Setter {}: {}", (kat.alvorlighetsgrad == Alvorlighetsgrad.FEIL ? "feil" : "varsel"), melding);
        };
    }

    /** Legger til en feil eller varsel på responsen som skal returneres og avbryter videre regelkjøring. */
    public static final Runnable leggTilMeldingOgAvbryt(Kategori kat, String melding) {
        return () -> {
            leggTilMelding(kat, melding).run();
            avbrytRegelkjøring.run();
        };
    }
    
    /** Avbryter regelkjøringen i stillhet. */
    public static final Runnable avbrytRegelkjøring = () -> {
        loggInfo("Avbryter regelkjøring");
        throw new AvbrytRegelkjoeringIStillhetException();
    };
    
    /** Setter en variabel (beregnet verdi). */
    public static final Runnable settArgument(Argument variabel, Verdielement verdi) {
        return settArgument(variabel, verdi.verdi());
    }
    
    /** Setter en variabel (beregnet verdi). */
    public static final Runnable settArgument(Argument variabel, Object verdi) {
        return () -> {
            KontekstManager.settVariabel(variabel, verdi);
        };
    }
    
    /** Kommando som legger til en lovvalgsbestemmelse for en artikkel. */
    public static final Runnable opprettLovvalgbestemmelse(Artikkel artikkel, Betingelse... betingelser) {
        return () -> {
            Lovvalgsbestemmelse lb = new Lovvalgsbestemmelse();
            lb.artikkel = artikkel;
            lb.betingelser = new ArrayList<>();
            if (responsen().lovvalgsbestemmelser.contains(lb)) {
                loggError("Forsøk på å opprette eksisterende artikkel {}", artikkel);
                leggTilMeldingOgAvbryt(TEKNISK_FEIL, "Teknisk feil i regelmodulen").run();
            }
            responsen().lovvalgsbestemmelser.add(lb);
            loggInfo("Opprettet lovvalgsbestemmelse for artikkel {}", artikkel);
            for (Betingelse bet : betingelser) {
                lb.betingelser.add(bet);
                loggInfo("  med betingelse \"{}\" som {}", bet.argument, bet.resultat);
            }
        };
    }
    
}
