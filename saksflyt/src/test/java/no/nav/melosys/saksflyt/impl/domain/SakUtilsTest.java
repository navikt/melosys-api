package no.nav.melosys.saksflyt.impl.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import no.nav.melosys.saksflyt.api.Status;

public class SakUtilsTest {
    
    @Test
    public void testPredicateSakMedStatus() {
        SakImpl s = new SakImpl();
        assertFalse(SakUtils.sakMedStatus(Status.SOEKA1_V1_KLARGJORT).test(s));
        s.setStatus(Status.SOEKA1_V1_NY);
        assertFalse(SakUtils.sakMedStatus(Status.SOEKA1_V1_KLARGJORT).test(s));
        s.setStatus(Status.SOEKA1_V1_KLARGJORT);
        assertTrue(SakUtils.sakMedStatus(Status.SOEKA1_V1_KLARGJORT).test(s));
    }
    
    @Test
    public void testComparatorKortestFristFørst() {
        SakImpl s1 = new SakImpl(), s2 = new SakImpl();
        s1.setFristDato(LocalDate.of(2017, 01, 01));
        s2.setFristDato(LocalDate.of(2017, 01, 02));
        assertTrue(SakUtils.kortestFristFørst().compare(s1, s1) == 0);
        assertTrue(SakUtils.kortestFristFørst().compare(s1, s2) < 0);
        assertTrue(SakUtils.kortestFristFørst().compare(s2, s1) > 0);
    }

}
