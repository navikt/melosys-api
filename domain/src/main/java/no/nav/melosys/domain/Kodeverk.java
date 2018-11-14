package no.nav.melosys.domain;

/**
 * Felles interface for kodeverk som hardkodes.
 *
 * IKKE implementer denne klassen for kodeverk med mindre forretningsregler eller -logikk må forholde seg til kodeverket.
 *
 * Altså: Kodeverk som ikke har innvirkning på utfall (f.eks. kjønn), skal lagres som String (bruk KodeverkService til å gjøre oppslag).
 *
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
