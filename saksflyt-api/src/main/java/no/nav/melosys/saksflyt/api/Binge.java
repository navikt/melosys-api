package no.nav.melosys.saksflyt.api;

import java.util.Collection;
import java.util.function.Predicate;

import no.nav.melosys.domain.Prosessinstans;


/**
 * Shared object space for alle prosessinstanser.
 *
 * Implementasjonen tilbyr operasjoner for å legge til, hente og fjerne prosessinstanser.
 *
 * Å legge til prosessinstanser putter dem inn i bingen. Å hente prosessinstanser vil vise hva som finnes i bingen (uten å fjerne dem).
 * Å fjerne prosessinstanser vil hente ut prosessinstanser og samtidig fjerne dem fra bingen.
 *
 */
public interface Binge {

    /**
     * Legger til en prosessinstans hvis det allerede ikke finnes en prosessinstans i bingen med samme prosessinstansId.
     *
     * @return True hvis prosessinstansen ble lagt til
     */
    boolean leggTil(Prosessinstans prosessinstans);

    /**
     * Henter alle prosessinstanser - både de som venter og som blir utført
     */
    Collection<Prosessinstans> hentProsessinstanser();

    /**
     * Fjerner den første prosessinstansen som tilfredsstiller det gitte predikatet
     * fra ventende prosessinstanser og legger den til blant utførende prosessinstanser.
     * Returnerer prosessinstansen eller null hvis ingen prosessinstanser tilfredsstiller kriteriet.
     */
    Prosessinstans hentOgSettProsessinstansTilAktiv(Predicate<Prosessinstans> predikat);

    /**
     * Fjerner prosessinstansen fra aktive prosessinstanser
     */
    void fjernFraAktiveProsessinstanser(Prosessinstans prosessinstans);
}
