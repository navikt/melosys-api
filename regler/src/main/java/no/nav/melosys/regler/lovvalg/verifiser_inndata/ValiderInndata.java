package no.nav.melosys.regler.lovvalg.verifiser_inndata;


import static no.nav.melosys.domain.dokument.person.Personstatus.DØD;
import static no.nav.melosys.domain.dokument.person.Personstatus.DØDD;
import static no.nav.melosys.regler.api.lovvalg.rep.Kategori.IKKE_STOETTET;
import static no.nav.melosys.regler.lovvalg.LovvalgKommandoer.leggTilMelding;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.personopplysningDokumentet;
import static no.nav.melosys.regler.motor.voc.Deklarasjon.hvis;
import static no.nav.melosys.regler.motor.voc.Verdielement.verdien;

import no.nav.melosys.regler.motor.Regelpakke;

public final class ValiderInndata implements Regelpakke {
    
    /** Gi feil hvis bruker er død. */
    @Regel
    public static void giVarselHvisBrukerErDød() {
        hvis(verdien(personopplysningDokumentet().personstatus).erEnAv(DØD, DØDD))
        .så(leggTilMelding(IKKE_STOETTET, "Bruker er død"));
    }
    
    
    // FIXME: Mer validering
    
    /*
    private static void verifiserKonsistens() {
        // Verifiser flere land
        if (søknad().arbeidFlereLand && søknad().land.size() < 2) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Oppgitt arbeid i flere land, men kun ett land i søknaden");
        }
        if (!søknad().arbeidFlereLand && søknad().land.size() > 1) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Oppgitt arbeid i ett land, men flere land i søknaden");
        }
        // Verifiser unike land
        HashSet<String> land = new HashSet<>();
        for (String s : søknad().land) {
            if (!land.add(s)) {
                leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Landet '" + s + "' er oppgitt flere ganger");
            }
        }
        // Verifiser perioden
        if (søknad().periodeFom != null && søknad().periodeTom != null && søknad().periodeFom.isAfter(søknad().periodeTom)) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Oppgitt fra-dato er etter oppgitt til-dato");
        }
    }
*/

}
