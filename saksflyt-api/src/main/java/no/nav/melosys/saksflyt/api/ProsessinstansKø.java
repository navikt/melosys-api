package no.nav.melosys.saksflyt.api;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.saksflyt.Prosessinstans;

public interface ProsessinstansKø {

    /**
     * Henter alle prosessinstanser - både de som venter og som blir utført
     */
    Collection<Prosessinstans> hentProsessinstanser();

    /**
     * Legger til en prosessinstans om den ikke allerede eksisterer i bingen
     */
    boolean leggTil(Prosessinstans prosessinstans);

    /**
     * Plukker neste prosessinstans som skal behandles
     */
    Optional<Prosessinstans> plukkNeste();

}
