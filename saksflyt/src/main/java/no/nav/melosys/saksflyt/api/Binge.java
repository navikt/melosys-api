package no.nav.melosys.saksflyt.api;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Shared object space for alle saker under behandling.
 * 
 * Implementasjonen tilbyr operasjoner for å legge til, hente og fjerne saker.
 * 
 * Å legge til saker putter dem inn i bingen. 
 * Å hente saker vil vise hva som finnes i bingen (uten å fjerne dem).
 * Å fjerne saker vil hente ut saken og samtidig fjerne dem fra bingen.
 * 
 */
public interface Binge {

    /**
     * Legger til en sak hvis det allerede ikke finnes en sak i bingen med sanne saksId.
     *
     * @return True hvis saken ble lagt til
     */
    public boolean leggTilSak(Sak sak); 
    
    /**
     * Henter en sak med en gitt id uten å fjerne den fra lageret.
     * 
     * @return Angitt sak, eller null hvis ingen slik sak i lageret.
     */
    public Sak hentSak(long saksId);

    /**
     * Henter alle saker som tilfredsstiller et predikat fra lageret (uten å fjerne dem fra lageret).
     */
    public Collection<Sak> hentSaker(Predicate<Sak> predikat);
    
    /**
     * Henter alle saker som tilfredsstiller et predikat fra lageret (uten å fjerne dem fra lageret). Listen sorteres etter et gitt kriterium.
     */
    public List<Sak> hentSaker(Predicate<Sak> predikat, Comparator<Sak> rekkefølge);

    /**
     * Fjerner en sak med en gitt id fra lageret.
     * 
     * @return Angitt sak, eller null hvis ingen slik sak i lageret.
     */
    public Sak fjernSak(long saksId);

    /**
     * Fjerner og returnerer den første saken som tilfredsstiller det gitte predikatet, eller null hvis ingen saker tilfredsstiller kriteriet.
     */
    public Sak fjernFørsteSak(Predicate<Sak> predikat, Comparator<Sak> rekkefølge);

}
