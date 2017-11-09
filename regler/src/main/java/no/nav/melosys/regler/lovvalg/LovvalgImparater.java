package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.motor.RegelLogg.loggInfo;

import no.nav.melosys.regler.api.lovvalg.rep.Alvorlighetsgrad;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.rep.Kategori;

public final class LovvalgImparater {

    private LovvalgImparater() {}

    /** Legger til en feil eller varsel på responsen som skal returneres, og skriver til logg. */
    public static Runnable leggTilMelding(Kategori kat, String melding) {
        return () -> {
            Feilmelding feil = new Feilmelding();
            feil.kategori = kat;
            feil.feilmelding = melding;
            LovvalgKontekstManager.responsen().feilmeldinger.add(feil);
            loggInfo("Setter {}: {}", (kat.alvorlighetsgrad == Alvorlighetsgrad.FEIL ? "feil" : "varsel"), melding);
        };
    }

}
