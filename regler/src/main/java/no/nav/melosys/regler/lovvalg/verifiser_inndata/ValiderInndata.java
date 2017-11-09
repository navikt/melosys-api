package no.nav.melosys.regler.lovvalg.verifiser_inndata;


import no.nav.melosys.regler.motor.Regelpakke;

public final class ValiderInndata extends Regelpakke {
    
    // FIXME: Revisjon
    
    /*
    private static void verifiserKonsistens() {
        // Veriser flere land
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
