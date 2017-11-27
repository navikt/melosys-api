package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.responsen;
import static no.nav.melosys.regler.motor.RegelLogg.loggInfo;

import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.Argument;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.rep.Kategori;
import no.nav.melosys.regler.motor.AvbrytRegelkjoeringIStillhetException;
import no.nav.melosys.regler.motor.KontekstManager;

/**
 * Klassen inneholder verbalisering av kommandoer
 */
public final class LovvalgImparater {

    private LovvalgImparater() {}

    /** Legger til en feil eller varsel på responsen som skal returneres, og skriver til logg. */
    public static Runnable leggTilMelding(Kategori kat, String melding) {
        return () -> {
            Feilmelding feil = new Feilmelding();
            feil.kategori = kat;
            feil.feilmelding = melding;
            responsen().feilmeldinger.add(feil);
            loggInfo("Setter {}: {}", (kat.alvorlighetsgrad == Alvorlighetsgrad.FEIL ? "feil" : "varsel"), melding);
        };
    }

    /** Legger til en feil eller varsel på responsen som skal returneres, skriver til logg og avbryter videre regelkjøring. */
    public static Runnable leggTilMeldingOgAvbryt(Kategori kat, String melding) {
        return () -> {
            leggTilMelding(kat, melding).run();
            avbrytRegelkjøring.run();
        };
    }
    
    /** Setter en variabel (beregnet verdi) */
    public static Runnable settVariabel(Argument variabel, Object verdi) {
        return () -> {
            KontekstManager.settVariabel(variabel, verdi);
        };
    }
    
    /** Avbryter regelkjøringen i stillhet */
    public static final Runnable avbrytRegelkjøring = () -> {
        loggInfo("Avbryter regelkjøring");
        throw new AvbrytRegelkjoeringIStillhetException();
    };
    
}
