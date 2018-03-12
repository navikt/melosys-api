package no.nav.melosys.saksflyt.impl.binge;

import static no.nav.melosys.domain.ProsessSteg.A001_HENT_ARBF_OPPL;
import static no.nav.melosys.domain.ProsessSteg.A001_JOURF;
import static org.junit.Assert.*;

import java.time.LocalDateTime;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.saksflyt.impl.Utils;

public class BingeImplTest {

    /*
     * Tester at vi kan legge til saker, hente dem og fjerne dem.
     */
    @Test
    public void testGrunnleggendeFunksjonalitet() {
        BingeImpl binge = new BingeImpl();
        Prosessinstans pi1 = new Prosessinstans(), pi2 = new Prosessinstans();
        ReflectionTestUtils.setField(pi1, "id", 1L);
        ReflectionTestUtils.setField(pi2, "id", 2L);
        pi1.setRegistrertDato(LocalDateTime.of(2017, 1, 2, 0, 0));
        pi2.setRegistrertDato(LocalDateTime.of(2017, 1, 1, 0, 0));
        pi1.setSteg(A001_JOURF);
        pi2.setSteg(A001_HENT_ARBF_OPPL);
        assertTrue(binge.leggTil(pi1));
        assertTrue(binge.leggTil(pi2));
        assertFalse(binge.leggTil(pi2)); // Skal ikke kunne legge til samme sak flere ganger
        assertNull(binge.hentProsessinstans(0)); // Ingen sak med saksId 0 er lagt inn
        assertEquals(2, binge.hentProsessinstanser((s) -> true).size()); // Skal være 2 saker i bingen
        assertEquals(0, binge.hentProsessinstanser((s) -> false).size()); // Skal ikke returnere noe hvis predikatet ikke slår til
        assertEquals(pi2, binge.fjernFørsteProsessinstans((s) -> true, Utils.eldsteFørst())); // b2 har kortest frist
        assertEquals(pi1, binge.fjernFørsteProsessinstans((s) -> true, Utils.eldsteFørst())); // Nå er det b1 som har kortest frist
        assertNull(binge.fjernFørsteProsessinstans((s) -> true, Utils.eldsteFørst())); // Alle sakene er hentet ut. Fjern skal returnere null
    }

}
