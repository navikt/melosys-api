package no.nav.melosys.saksflyt.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import no.nav.melosys.domain.Behandling;

/**
 * Shared object space for alle behandlinger.
 * 
 * Implementasjonen tilbyr operasjoner for å legge til, hente og fjerne behandlinger.
 * 
 * Å legge til behandlinger putter dem inn i bingen. Å hente behandlinger vil vise hva som finnes i bingen (uten å fjerne dem).
 * Å fjerne behandlinger vil hente ut behandlingen og samtidig fjerne dem fra bingen.
 * 
 */
public interface Binge {

    /**
     * Legger til en behandling hvis det allerede ikke finnes en behandling i bingen med samme behandlingsId.
     *
     * @return True hvis behandlingen ble lagt til
     */
    public boolean leggTil(Behandling behandling);

    /**
     * Henter en behandling med en gitt id uten å fjerne den fra lageret.
     * 
     * @return Angitt behandling, eller null hvis ingen slik behandling i lageret.
     */
    public Behandling hentBehandling(long behandlingsId);

    /**
     * Henter alle behandlinger som tilfredsstiller et predikat fra lageret (uten å fjerne dem fra lageret).
     */
    public Collection<Behandling> hentBehandlinger(Predicate<Behandling> predikat);

    /**
     * Henter alle behandlinger som tilfredsstiller et predikat fra lageret (uten å fjerne dem fra lageret). Listen sorteres
     * etter et gitt kriterium.
     */
    public List<Behandling> hentBehandlinger(Predicate<Behandling> predikat, Comparator<Behandling> rekkefølge);

    /**
     * Fjerner en behandling med en gitt id fra lageret.
     * 
     * @return Angitt behandling, eller null hvis ingen slik behandling i lageret.
     */
    public Behandling fjernBehandling(long behandlingsId);

    /**
     * Fjerner og returnerer den første behandlingen som tilfredsstiller det gitte predikatet, eller null hvis ingen
     * behandlinger tilfredsstiller kriteriet.
     */
    public Behandling fjernFørsteBehandling(Predicate<Behandling> predikat);

    /**
     * Fjerner og returnerer den første behandlingen som tilfredsstiller det gitte predikatet, eller null hvis ingen
     * behandlinger tilfredsstiller kriteriet. Listen sorteres etter et gitt kriterium.
     */
    public Behandling fjernFørsteBehandling(Predicate<Behandling> predikat, Comparator<Behandling> rekkefølge);

}
