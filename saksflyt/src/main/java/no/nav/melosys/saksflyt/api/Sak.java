package no.nav.melosys.saksflyt.api;

import java.time.LocalDate;

/**
 * Representasjon av en sak.
 * FIXME (farjam): Må avklare om dette skal være Sak eller Behandling
 */
public interface Sak {

    /**
     * Returnerer denne sakens unike id.
     */
    public long getSaksId();
    
    /**
     * Returnerer denne sakens status.
     */
    public Status getStatus();
    
    /**
     * Returnerer sakens frist.
     */
    public LocalDate getFristDato();
    
    /**
     * Returnerer sakens registreringsdato.
     */
    public LocalDate getRegistrertDato();
    
    /**
     * Minimal implementasjon for å legge på merknader.
     * FIXME (Farjam): Avventer dbmodell
     */
    public void leggTilMerknad(String Merknad);
    
}
