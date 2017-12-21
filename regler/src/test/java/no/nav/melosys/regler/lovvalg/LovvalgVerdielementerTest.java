package no.nav.melosys.regler.lovvalg;

import static no.nav.melosys.regler.lovvalg.LovvalgVerdielementer.antallMånederI;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.Test;

import no.nav.melosys.domain.ErPeriode;

public class LovvalgVerdielementerTest {
    
    private static class Periode implements ErPeriode {
        LocalDate fom, tom;
        Periode(int få, int fm, int fd, int tå, int tm, int td) {
            this.fom = LocalDate.of(få, fm, fd);
            this.tom = LocalDate.of(tå, tm, td);
        }
        @Override public LocalDate getFom() {
            return fom;
        }
        @Override public LocalDate getTom() {
            return tom;
        }
    }

    @Test
    public void testAntallmånederI() {
        // 24 måneder i perioden 05.05.2017 → 04.05.2019
        assertEquals(24L, antallMånederI(new Periode(2017, 5, 5, 2019, 5, 4)).verdi());

        // 25 måneder i perioden 05.05.2017 → 05.05.2019
        assertEquals(25L, antallMånederI(new Periode(2017, 5, 5, 2019, 5, 5)).verdi());
    }
    
}
