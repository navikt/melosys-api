
package no.nav.melosys.regler.motor.dekl;

import static no.nav.melosys.regler.motor.dekl.Verdielement.*;
import static no.nav.melosys.regler.motor.dekl.VerdielementSett.forAlle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class VerdielementSettTest {
    
    // Hierarkisk datastruktur for å teste nøstede VerdielementSett...
    private static class Verden {List<Avdeling> avdelinger; Verden(Avdeling... a) {avdelinger = Arrays.asList(a);}}
    private static class Avdeling {List<Faksjon> faksjoner; Avdeling(Faksjon... f) {faksjoner = Arrays.asList(f);}}
    private static class Faksjon {List<Innbygger> innbyggere; Faksjon(int antInnb) {innbyggere = new ArrayList<>(); for (int i = 0; i < antInnb; i++) innbyggere.add(new Innbygger());}}
    private static class Innbygger {boolean harHoppet = false; void hopp() {harHoppet = true;}}

    /**
     * 
     */
    @Test
    public void testNøstedeElementsett() {
        Verden univers = settOppDisneyUnivers();
        forAlle(univers.avdelinger)
        .sine(a -> {return a.faksjoner;})
        .sine(s -> {return s.innbyggere;})
        .utfør(i -> {
            assertFalse(i.harHoppet); // Skal bare skje en gang per innbygger 
            i.hopp();
        });
        // Sjekk at alle har hoppet
        for (Avdeling a : univers.avdelinger)
            for (Faksjon f : a.faksjoner)
                for (Innbygger i : f.innbyggere)
                    assertTrue(i.harHoppet);
        // Antallet som har hoppet skal være 154
        assertTrue(antallet(forAlle(univers.avdelinger)
            .sine(s -> {return s.faksjoner;})
            .sine(s -> {return s.innbyggere;})
            .som(i -> {return i.harHoppet;}))
            .erLik(154).test());
        // Antallet som ikke har hoppet skal være 0
        assertTrue(antallet(forAlle(univers.avdelinger)
            .sine(s -> {return s.faksjoner;})
            .sine(s -> {return s.innbyggere;})
            .som(i -> {return !i.harHoppet;}))
            .erLik(0).test());
    }
    
    private Verden settOppDisneyUnivers() {
        // StarWars...
        Faksjon irriterendeFurballs = new Faksjon(150); // Samtlige ewoker i universet
        Faksjon ikkeIrriterendeFurBalls = new Faksjon(1); // Chewbaka
        Faksjon alleKvinnenerIFørsteTriologi = new Faksjon(1); // Leia
        Avdeling starWarsReservat = new Avdeling(irriterendeFurballs, ikkeIrriterendeFurBalls, alleKvinnenerIFørsteTriologi);
        // Pirates of the Careebean
        Faksjon alleKvinnenerIFørsteFilm = new Faksjon(1); // Elizabeth Swann
        Faksjon soldaterViBlirKjentMed = new Faksjon(0);
        Faksjon klassiskePiraterMedPappegøye = new Faksjon(1);
        Avdeling piratesOfTheCareebeanLand = new Avdeling(alleKvinnenerIFørsteFilm, soldaterViBlirKjentMed, klassiskePiraterMedPappegøye);
        Verden disneyland = new Verden(starWarsReservat, piratesOfTheCareebeanLand);
        return disneyland;
    }
    
}
