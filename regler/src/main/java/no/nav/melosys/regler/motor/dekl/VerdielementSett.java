/**
 * 
 */
package no.nav.melosys.regler.motor.dekl;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import no.nav.melosys.regler.motor.Predikat;

/**
 * Klasse som støtter verbalisering av mengder og sett.
 * 
 * Klassen støtter nøstede verdisett
 * 
 * @param <T> Typen til elementene
 * @param <S> Typen til de overordnede elementene (hvis nøstet sett)
 */
public class VerdielementSett<T, S> implements Iterable<T> {
    
    Iterable<T> verdieelementer;
    List<Predicate<? super T>> predikater = new ArrayList<>();
    
    // Støtte for nøstede sett
    private VerdielementSett<S, ?> overordnetVerdielementSett;
    private Function<S, Iterable<T>> nøstetSupplier;

    private VerdielementSett() {}
    
    /**
     * Lager en VerdielementSett baser på en Iterable
     */
    public static <T> VerdielementSett<T, ?> forAlle(Iterable<T> samling) {
        VerdielementSett<T, ?> vs = new VerdielementSett<>();
        vs.verdieelementer = samling;
        return vs;
    }

    /**
     * Legger til et filter på dette settet.
     */
    public VerdielementSett<T, S> som(Predicate<? super T> predikat) {
        predikater.add(predikat);
        return this;
    }
    
    /**
     * Lager en nøstet VerdielementSett, basert på en map-funksjon
     */
    public <U> VerdielementSett<U, T> sine(Function<T, Iterable<U>> nøstetSupplier) {
        VerdielementSett<U, T> nvs = new VerdielementSett<>();
        nvs.overordnetVerdielementSett = this;
        nvs.nøstetSupplier = nøstetSupplier;
        return nvs;
    }

    /**
     * Utfører en kommando for alle elementene.
     */
    public void utfør(Consumer<? super T> kommando) {
        for (T t : this) {
            kommando.accept(t);
        }
    }
    
    /**
     * Returnerer en Iterator som itererer over alle verdiene.
     */
    @Override
    public Iterator<T> iterator() {
        Iterator<T> res = new Iterator<T>() {
            T neste = null;
            boolean harNeste = true; // Settes til true for å gjøre første iterering
            Iterator<T> denneIterator = overordnetVerdielementSett == null ? verdieelementer.iterator() : null;
            Iterator<S> superIterator = overordnetVerdielementSett == null ? null : overordnetVerdielementSett.iterator();
            @Override public boolean hasNext() {
                return harNeste;
            }
            @Override
            public T next() {
                if (!harNeste) {
                    throw new NoSuchElementException();
                }
                T nåværende = neste;
                outer: for (;;) {
                    if (denneIterator == null || !denneIterator.hasNext()) {
                        if (superIterator == null || !superIterator.hasNext()) {
                            harNeste = false;
                            neste = null;
                            return nåværende;
                        }
                        denneIterator = nøstetSupplier.apply(superIterator.next()).iterator(); 
                        continue; // I tilfelle den nye iteratoren ikke gir noen elementer
                    }
                    neste = denneIterator.next();
                    for (Predicate<? super T> predikat : predikater) {
                        if (!predikat.test(neste)) {
                            continue outer; // Hopp direkte til neste verdi hvis et predikat ikke slår til.
                        }
                    }
                    harNeste = true;
                    return nåværende;
                }
            }
        };
        try {res.next();} catch (NoSuchElementException e) {}
        return res;
    }

    /**
     * Predikat som evaluerer om settet inneholder et minimum antall verdier.
     */
    public Predikat inneholderMinst(int min) {
        return () -> {
            return antallElementer() >= min;
        };
    }
    
    /**
     * Predikat som evaluerer om settet inneholder færre enn (eller lik) et maximum antall verdier.
     */
    public Predikat inneholderMax(int max) {
        return () -> {
            return antallElementer() <= max;
        };
    }
    
    /**
     * Predikat som evaluerer om settet er tomt.
     */
    public Predikat erTomt() {
        return inneholderMax(0);
    }
    
    /**
     * Returnerer antall elementer i settet.
     */
    @SuppressWarnings("unused") 
    public int antallElementer() {
        int antTreff = 0;
        for (T t : this) {
            antTreff++;
        }
        return antTreff;
    }
    
}
