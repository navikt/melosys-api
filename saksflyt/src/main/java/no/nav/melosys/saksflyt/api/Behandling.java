package no.nav.melosys.saksflyt.api;

import java.time.LocalDate;

import no.nav.melosys.domain.BehandlingStatus;

/**
 * Representasjon av en behandling. TODO trenges?
 */
public interface Behandling {

    /**
     * Returnerer denne behandlingens unike id.
     */
    public long getSaksId();

    /**
     * Returnerer denne behandlingens status.
     */
    public BehandlingStatus getStatus();

    /**
     * Returnerer behandlingens frist.
     */
    public LocalDate getFristDato();

    /**
     * Returnerer behandlingens registreringsdato.
     */
    public LocalDate getRegistrertDato();

    /**
     * Minimal implementasjon for å legge på merknader. FIXME (Farjam): Avventer dbmodell
     */
    public void leggTilMerknad(String Merknad);

}
