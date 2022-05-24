package no.nav.melosys.service.kontroll.overlapp;


import java.time.LocalDate;

import no.nav.melosys.domain.dokument.medlemskap.Periode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodeOverlappSjekkTest {

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperKontrollperiode_med2ÅrOg1Dag_2DagerOverlapEtterÅrsOverlap_false() {
        /*
            Overlap:                  2y   1d   2y
            medlemsperiode:        |------|::|
            kontrollperiode:              |::|------|
        */
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(medlemsperiode, kontrollperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollperiodeOverlapperMedlemsperiode_har1DagOverSammeStartOgSluttDagPå2År_false() {
        /*
            Overlap:                   2y   1d  2y
            kontrollperiode:               |::|------|
            medlemsperiode:         |------|::|
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperKontrollperiode_har2DagerOverSammeStartOgSluttDagPå2År_true() {
        /*
            Overlap:                   2y  1d+1d  2y
            medlemsperiode:        |------|::|::|
            kontrollperiode:              |::|::|------|
        */
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(2));
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(medlemsperiode, kontrollperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollperiodeOverlapperMedlemsperiode_har2DagerOverSammeStartOgSluttDagPå2År_true() {
        /*
            Overlap:                   2y   1d+1d  2y
            kontrollperiode:               |::|::|------|
            medlemsperiode:         |------|::|::|
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperMedKontrollperiode_harSammeStartOgSluttDagPå2År_false() {
        /*
            Overlap:                   2y     2y
            kontrollperiode:        |------|
            medlemsperiode:                |------|
        */
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(medlemsperiode, kontrollperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_harSammeStartOgSluttDagPå2År_false() {
                /*
            Overlap:                   2y     2y
            kontrollperiode:               |------|
            medlemsperiode:         |------|
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperIkkeKontrollperiode_harIngenOverlapp_false() {
        /*
            Overlap:                   1y    (1d)   ~1y
            kontrollperiode:        |------|
            medlemsperiode:                      |------|
        */
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1));
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(1).plusDays(1), LocalDate.EPOCH.plusYears(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(medlemsperiode, kontrollperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperIkkeMedlemsperiode_harIngenOverlapp_false() {
                /*
            Overlap:                   1y   (1d)   ~1y
            kontrollperiode:                    |------|
            medlemsperiode:         |------|
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(1).plusDays(1), LocalDate.EPOCH.plusYears(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperMedKontrollperiode_harSammeStartOgSluttDagPå1År_false() {
        /*
            Overlap:                   1y     1y
            kontrollperiode:        |------|
            medlemsperiode:                |------|
        */
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1));
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(medlemsperiode, kontrollperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_harSammeStartOgSluttDagPå1År_false() {
                /*
            Overlap:                   1y     1y
            kontrollperiode:               |------|
            medlemsperiode:         |------|
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }


    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med1DagOverlappingOver1Dag_false() {
        /*
            Overlap:
            kontrollperiode:        |::| 1d
            medlemsperiode:         |::| 1d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med1DagOverlappingStartenAv10Dager_false() {
        /*
            Overlap:                 1d
            kontrollperiode:        |::|
            medlemsperiode:         |::|--|--|--|--|--|--|--|--|--| 10d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med1DagOverlappingMellom1Dag_false() {
        /*
            Overlap:                          1d
            kontrollperiode:                 |::|
            medlemsperiode:         |--|--|--|::|--|--|--|--|--|--| 10d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusDays(3), LocalDate.EPOCH.plusDays(4));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med2DagOverlappingMellom10Dager_true() {
        /*
            Overlap:                          1d+1d
            kontrollperiode:                 |::|::|
            medlemsperiode:         |--|--|--|::|::|--|--|--|--|--| 10d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusDays(3), LocalDate.EPOCH.plusDays(5));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_medOver1DagOverlappingOver3Dager_true() {
        /*
            Overlap:                    1d
            kontrollperiode:        |--|::|
            medlemsperiode:            |::|--|
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH.plusDays(1), LocalDate.EPOCH.plusDays(3));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med2DagerOverlappingOver2Dager_true() {
        /*
            Overlap:                 1d+1d
            kontrollperiode:        |::|::|
            medlemsperiode:         |::|::|
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }
}
