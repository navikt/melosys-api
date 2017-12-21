package no.nav.melosys.regler.motor.voc;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
    private Iterable<S> overordnetVerdielementSett;
    private Function<S, Iterable<T>> nøstetSupplier;

    private VerdielementSett() {}
    
    /**
     * Lager en VerdielementSett baser på en Iterable.
     */
    public static <T> VerdielementSett<T, ?> forAlle(Iterable<T> samling) {
        return alle(samling);
    }

    /**
     * Lager en VerdielementSett baser på en Iterable
     */
    public static <T> VerdielementSett<T, ?> alle(Iterable<T> samling) {
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
     * Returnerer en ny VerdielementSett som er lik denne, men uten duplikater. Metoden bevarer ikke rekkefølgen til elementene.
     */
    public VerdielementSett<T, ?> somErUnike() {
        HashSet<T> unike = new HashSet<>();
        utfør(unike::add);
        return alle(unike);
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
        /*
         * ADVARSEL: UTFORDRENDE KODE.
         */
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
        res.next();
        return res;
    }
    
}
