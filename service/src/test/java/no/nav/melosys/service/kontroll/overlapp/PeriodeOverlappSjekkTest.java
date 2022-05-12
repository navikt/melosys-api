package no.nav.melosys.service.kontroll.overlapp;


import java.time.LocalDate;

import no.nav.melosys.domain.dokument.medlemskap.Periode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodeOverlappSjekkTest {

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperMedKontrollPeriode_medOver1DagOverlapping_true() {
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(medlemsperiode, kontrollperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperMedKontrollPeriode_harSammeDagOver2År_false() {
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(medlemsperiode, kontrollperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_medOver1DagOverlapping_true() {
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_harSammeDagOver2År_false() {
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }
}
