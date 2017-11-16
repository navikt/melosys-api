
package no.nav.melosys.regler.motor.dekl;

import static no.nav.melosys.regler.motor.dekl.VerdielementSett.forAlle;
import static no.nav.melosys.regler.motor.dekl.VerdielementSett.settet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;


public class VerdielementSettTest {
    
    // Hierarkisk datastruktur for å teste nøstede VerdielementSett...
    private static class Verden {Avdeling [] avdelinger; Verden(Avdeling... f) {avdelinger = f;}}
    private static class Avdeling {Faksjon[] faksjoner; Avdeling(Faksjon... k) {faksjoner = k;}}
    private static class Faksjon {Innbygger[] innbyggere; Faksjon(int antInnb) {innbyggere = new Innbygger[antInnb]; for (int i = 0; i < antInnb; i++) innbyggere[i] = new Innbygger();}}
    private static class Innbygger {boolean harHoppet = false; void hopp() {harHoppet = true;}}

    @Test
    public void testNøstedeElementsett() {
        Verden univers = settOppDisneyUnivers();
        forAlle(Arrays.asList(univers.avdelinger))
        .sine(s -> {return Arrays.asList(s.faksjoner);})
        .sine(s -> {return Arrays.asList(s.innbyggere);})
        .utfør(i -> {i.hopp();});
        // Sjekk at alle har hoppet
        for (Avdeling a : univers.avdelinger)
            for (Faksjon f : a.faksjoner)
                for (Innbygger i : f.innbyggere)
                    assertTrue(i.harHoppet);
        // Antallet som har hoppet skal være 154
        assertEquals(154, settet(Arrays.asList(univers.avdelinger))
            .sine(s -> {return Arrays.asList(s.faksjoner);})
            .sine(s -> {return Arrays.asList(s.innbyggere);})
            .som(i -> {return i.harHoppet;})
            .antallElementer());
        // Antallet som ikke har hoppet skal være 0
        assertEquals(0, settet(Arrays.asList(univers.avdelinger))
            .sine(s -> {return Arrays.asList(s.faksjoner);})
            .sine(s -> {return Arrays.asList(s.innbyggere);})
            .som(i -> {return !i.harHoppet;})
            .antallElementer());
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
