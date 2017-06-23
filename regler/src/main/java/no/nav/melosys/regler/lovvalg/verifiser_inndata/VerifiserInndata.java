package no.nav.melosys.regler.lovvalg.verifiser_inndata;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.leggTilMeldingOgLogg;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.søknad;

import java.util.Collections;

import no.nav.melosys.regler.api.lovvalg.Kategori;

public final class VerifiserInndata {

    public static void kjørRegler() {
        // Sjekk om vi ha en søknad før all annen verifisering...
        if (søknad() == null) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Ingen søknad i forespørsel");
            return;
        }
        verifiserPåkrevdeFelter();
        verifiserKonsistens();
    }

    private static void verifiserPåkrevdeFelter() {
        // Sjekk flaggland...
        if (søknad().arbeidSkip && søknad().skipFlaggland == null) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Arbeid på skip, men ikke oppgitt flaggland");
        }
        
        // Sjekk sokkel-land
        if (søknad().arbeidSokkel && søknad().sokkelLand == null) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Arbeid på sokkel, men ikke oppgitt sokkelland");
        }
        
        // Sjekk periode
        if (søknad().periodeFom == null) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Søknaden mangler startdato");
        }
        if (søknad().periodeTom == null) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Søknaden mangler sluttdato");
        }
        
        // Sjekk land
        if (søknad().land == null) { // Kun for convenience for å slippe å sjekke for null i andre tester
            søknad().land = Collections.emptyList();
        }
        if (søknad().land.isEmpty()) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Ingen land oppgitt i søknaden");
        }
    }

    private static void verifiserKonsistens() {
        // Veriser flere land
        if (søknad().arbeidFlereLand && søknad().land.size() < 2) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Oppgitt arbeid i flere land, men kun ett land i søknaden");
        }
        if (!søknad().arbeidFlereLand && søknad().land.size() > 1) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Oppgitt arbeid i ett land, men flere land i søknaden");
        }
        // Verifiser perioden
        if (søknad().periodeFom != null && søknad().periodeTom != null && søknad().periodeFom.isAfter(søknad().periodeTom)) {
            leggTilMeldingOgLogg(Kategori.FEIL_I_SOEKNAD, "Oppgitt fra-dato er etter oppgitt til-dato");
        }
    }

}
