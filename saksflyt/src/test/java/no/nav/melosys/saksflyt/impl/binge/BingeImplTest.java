package no.nav.melosys.saksflyt.impl.binge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import no.nav.melosys.saksflyt.api.Status;
import no.nav.melosys.saksflyt.impl.domain.SakImpl;
import no.nav.melosys.saksflyt.impl.domain.SakUtils;

public class BingeImplTest {
    
    /*
     * Tester at vi kan legge til saker, hente dem og fjerne dem.
     */
    @Test
    public void testGrunnleggendeFunksjonalitet() {
        BingeImpl binge = new BingeImpl();
        SakImpl s1 = new SakImpl(), s2 = new SakImpl();
        ReflectionTestUtils.setField(s1, "saksId", 1);
        ReflectionTestUtils.setField(s2, "saksId", 2);
        s1.setFristDato(LocalDate.of(2017, 01, 02));
        s2.setFristDato(LocalDate.of(2017, 01, 01));
        s1.setStatus(Status.SOEKA1_V1_NY);
        s2.setStatus(Status.SOEKA1_V1_KLARGJORT);
        assertTrue(binge.leggTilSak(s1));
        assertTrue(binge.leggTilSak(s2));
        assertFalse(binge.leggTilSak(s2)); // Skal ikke kunne legge til samme sak flere ganger
        assertNull(binge.hentSak(0)); // Ingen sak med saksId 0 er lagt inn
        assertEquals(2, binge.hentSaker((s) -> true).size()); // Skal være 2 saker i bingen
        assertEquals(0, binge.hentSaker((s) -> false).size()); // Skal ikke returnere noe hvis predikatet ikke slår til
        assertEquals(s2, binge.fjernFørsteSak((s) -> true, SakUtils.kortestFristFørst())); // s2 har kortest frist
        assertEquals(s1, binge.fjernFørsteSak((s) -> true, SakUtils.kortestFristFørst())); // Nå er det s1 som har kortest frist
        assertNull(binge.fjernFørsteSak((s) -> true, SakUtils.kortestFristFørst())); // Alle sakene er hentet ut. Fjern skal returnere null
    }

}
