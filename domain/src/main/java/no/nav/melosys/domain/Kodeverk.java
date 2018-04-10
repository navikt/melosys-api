package no.nav.melosys.domain;

/**
 * Felles interface for alle enums som korresponderer til kodeverk.
 */
public interface Kodeverk {

    /**
     * Returnerer koden for enumen.
     */
    String getKode();

    /**
     * Returnerer en saksbehandler-vennlig beskrivelse av enumen.
     */
    String getBeskrivelse();

}
