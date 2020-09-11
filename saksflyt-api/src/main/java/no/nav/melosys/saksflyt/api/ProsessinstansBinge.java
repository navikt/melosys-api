package no.nav.melosys.saksflyt.api;

import java.util.Optional;

import no.nav.melosys.domain.saksflyt.Prosessinstans;

public interface ProsessinstansBinge {

    /**
     * Legger til en prosessinstans om den ikke allerede eksisterer i bingen
     */
    boolean leggTil(Prosessinstans prosessinstans);

    /**
     * Plukker neste prosessinstans som skal behandles
     */
    Optional<Prosessinstans> plukkNeste();

}
