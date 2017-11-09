/**
 * 
 */
package no.nav.melosys.regler.motor.dekl;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * FIXME: Revisjon
 *
 */
public class VerdielementSamling<T> {
    
    Iterable<T> verdieelementer;
    
    List<Predicate<? super T>> predikater = new ArrayList<>();
    
    private VerdielementSamling() {}
    
    public static <T> VerdielementSamling<T> forAlle(Iterable<T> samling) {
        VerdielementSamling<T> vs = new VerdielementSamling<>();
        vs.verdieelementer = samling;
        return vs;
    }
    
    public VerdielementSamling<T> hvor(Predicate<? super T> predikat) {
        predikater.add(predikat);
        return this;
    }
    
    public void utfør(Consumer<? super T> kommando) {
        outer: for (T verdi : verdieelementer) {
            for (Predicate<? super T> predikat : predikater) {
                if (!predikat.test(verdi)) {
                    continue outer; // Hopper direkte til neste verdi hvis et predikat ikke slår til.
                }
            }
            kommando.accept(verdi);
        }
    }
    
}
