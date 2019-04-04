package no.nav.melosys.saksflyt.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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
     * Henter en prosessinstans med en gitt id uten å fjerne den fra lageret.
     * 
     * @return Angitt prosessinstans, eller null hvis ingen slik prosessinstans i lageret.
     */
    Prosessinstans hentProsessinstans(long prosessinstansId);

    /**
     * Henter alle prosessinstanser
     */
    List<Prosessinstans> hentProsessinstanser();

    /**
     * Henter alle prosessinstanser som tilfredsstiller et predikat fra lageret (uten å fjerne dem fra lageret).
     */
    Collection<Prosessinstans> hentProsessinstanser(Predicate<Prosessinstans> predikat);

    /**
     * Henter alle prosessinstanser som tilfredsstiller et predikat fra lageret (uten å fjerne dem fra lageret). Listen sorteres etter et gitt kriterium.
     */
    List<Prosessinstans> hentProsessinstanser(Predicate<Prosessinstans> predikat, Comparator<Prosessinstans> rekkefølge);

    /**
     * Fjerner og returnerer den første prosessinstansen som tilfredsstiller det gitte predikatet, eller null hvis ingen
     * prosessinstanser tilfredsstiller kriteriet.
     */
    Prosessinstans fjernFørsteProsessinstans(Predicate<Prosessinstans> predikat);

    /**
     * Fjerner og returnerer den første prosessinstansen som tilfredsstiller det gitte predikatet, eller null hvis ingen
     * prosessinstanser tilfredsstiller kriteriet. Listen sorteres etter et gitt kriterium.
     */
    Prosessinstans fjernFørsteProsessinstans(Predicate<Prosessinstans> predikat, Comparator<Prosessinstans> rekkefølge);

}
