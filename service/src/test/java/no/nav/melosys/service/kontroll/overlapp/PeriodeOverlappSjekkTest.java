package no.nav.melosys.service.kontroll.overlapp;


import java.time.LocalDate;

import no.nav.melosys.domain.dokument.medlemskap.Periode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodeOverlappSjekkTest {

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperKontrollperiode_med2ÅrOg1Dag_true() {
        /*
            Overlap:                       1d
            medlemsperiode:        |------|::|              2y
            kontrollperiode:              |::|------|       2y
        */
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(medlemsperiode, kontrollperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollperiodeOverlapperMedlemsperiode_med2ÅrOg1Dag_true() {
        /*
            Overlap:                        1d
            kontrollperiode:               |::|------|          2y
            medlemsperiode:         |------|::|                 2y
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperKontrollperiode_har2DagerOverSammeStartOgSluttDagPå2År_true() {
        /*
            Overlap:                       1d+1d
            medlemsperiode:        |------|::|::|               2y
            kontrollperiode:              |::|::|------|        2y
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
            Overlap:                        1d+1d
            kontrollperiode:               |::|::|------|       2y
            medlemsperiode:         |------|::|::|              2y
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
            Overlap:                      1d
            kontrollperiode:        |------|            2y
            medlemsperiode:                |------|     2y
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
            Overlap:                       1d
            kontrollperiode:               |------|     2y
            medlemsperiode:         |------|            2y
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperMedKontrollperiode_harSammeStartOgSluttDagPå1År_false() {
        /*
            Overlap:                      1d
            kontrollperiode:        |------|            1y
            medlemsperiode:                |------|     1y
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
            Overlap:                      1d
            kontrollperiode:               |------|     1y
            medlemsperiode:         |------|            1y
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperIkkeKontrollperiode_harIngenOverlapp_false() {
        /*
            Overlap:                         (1d)
            kontrollperiode:        |------|                1y
            medlemsperiode:                      |------|   1y
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
            Overlap:                        (1d)
            kontrollperiode:                    |------|    1y
            medlemsperiode:         |------|                1y
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(1).plusDays(1), LocalDate.EPOCH.plusYears(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }


    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_medStartOgSluttOverlapperHverandre_true() {
        /*
            Overlap:                1d+1d
            kontrollperiode:        |:::|       2d
            medlemsperiode:         |:::|       2d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollperiodeOverlapperMedlemsperiode_medSammeSluttOgStartDatoOverlapp_false() {
        /*
            Overlap:                   1d
            kontrollperiode:        |--|        2d
            medlemsperiode:            |--|     2d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH.plusDays(1), LocalDate.EPOCH.plusDays(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_medlemsperiodeOverlapperKontrollperiode_medSammeSluttOgStartDatoOverlapp_false() {
        /*
            Overlap:                  1d
            kontrollperiode:           |--|     2d
            medlemsperiode:         |--|        2d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusDays(1), LocalDate.EPOCH.plusDays(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med1DagOverlappingStartenAv10Dager_true() {
        /*
            Overlap:                1d+1d
            kontrollperiode:        |::|                                 2d
            medlemsperiode:         |::|--|--|--|--|--|--|--|--|         10d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(1));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med1DagOverlappingSluttenAv10Dager_true() {
        /*
            Overlap:                                        1d+1d
            kontrollperiode:                                |::|     2d
            medlemsperiode:         |--|--|--|--|--|--|--|--|::|     10d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusDays(9), LocalDate.EPOCH.plusDays(10));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeSomOverlapperMedEnDag_mellomMedlemsperioden_false() {
        /*
            Overlap:                                        1d
            kontrollperiodePåEnDag                          |        1d
            medlemsperiode:         |--|--|--|--|--|--|--|--|--|     10d
        */
        Periode kontrollperiodePåEnDag = new Periode(LocalDate.EPOCH.plusDays(9), LocalDate.EPOCH.plusDays(9));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiodePåEnDag, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med2DagOverlappingMellom10Dager_true() {
        /*
            Overlap:                         1d+1d
            kontrollperiode:                 |::|                    2d
            medlemsperiode:         |--|--|--|::|--|--|--|--|--|     10d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusDays(3), LocalDate.EPOCH.plusDays(4));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med3DagerOverlappingMellom10Dager_true() {
        /*
            Overlap:                         1d+1d+1d
            kontrollperiode:                 |::|::|                 3d
            medlemsperiode:         |--|--|--|::|::|--|--|--|--|     10d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusDays(3), LocalDate.EPOCH.plusDays(5));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(10));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med2DagOverlappingOver3Dager_true() {
        /*
            Overlap:                   1d+1d
            kontrollperiode:        |--|::|         3d
            medlemsperiode:            |::|--|      3d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH.plusDays(1), LocalDate.EPOCH.plusDays(3));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollPeriodeOverlapperMedlemsperiode_med3DagerOverlappingOver3Dager_true() {
        /*
            Overlap:                1d+1d+1d
            kontrollperiode:        |::|::|     3d
            medlemsperiode:         |::|::|     3d
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusDays(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollperiodeHarÅpenOverlappMedMedlemsperiodePå2Dager_true() {
        /*
            Overlap:                       1d+1d
            kontrollperiode:               |::|-->          åpen periode
            medlemsperiode:         |------|::|             2y
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), null);
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollperiodeHarÅpenOverlappMedMedlemsperiodePå1Dag_false() {
        /*
            Overlap:                          1d
            kontrollperiode:                  |-->          åpen periode
            medlemsperiode:         |---------|             2y
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(2), null);
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollperiodeHar2DagerOverlappMedÅpenMedlemsperiode_true() {
        /*
            Overlap:                       1d+1d
            kontrollperiode:        |------|::|             2y
            medlemsperiode:                |::|-->          åpen periode
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH.plusYears(2), null);
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_kontrollperiodeHar1DagOverlappMedÅpenMedlemsperiode_false() {
        /*
            Overlap:                          1d
            kontrollperiode:        |---------|             2y
            medlemsperiode:                   |-->          åpen periode
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH.plusYears(2), null);
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertFalse(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_åpenMedlemsperiodeOverlapperHeleKontrollperioden_true() {
        /*
            Overlap:            1d          2y
            kontrollperiode:           |:::::::::|             2y
            medlemsperiode:      |------:::::::::->            åpen periode
        */
        Periode kontrollperiode = new Periode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(3));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH, null);
        PeriodeOverlappSjekk periodeOverlappSjekk = new PeriodeOverlappSjekk(kontrollperiode, medlemsperiode);


        boolean periodeOverlapperMerEnn1Dag = periodeOverlappSjekk.harPeriodeSomOverlapperMerEnn1Dag();


        assertTrue(periodeOverlapperMerEnn1Dag);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_manglerFomPåMedlemsperiode_kasterIllegalArgumentException() {
        Periode kontrollperiode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1));
        Periode medlemsperiodeUtenFom = new Periode(null, LocalDate.EPOCH.plusYears(2));

        assertThatThrownBy(() -> new PeriodeOverlappSjekk(kontrollperiode, medlemsperiodeUtenFom))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_manglerFomPåKontrollperiode_kasterIllegalArgumentException() {
        Periode kontrollperiodeUtenFom = new Periode(null, LocalDate.EPOCH.plusYears(1));
        Periode medlemsperiode = new Periode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(2));

        assertThatThrownBy(() -> new PeriodeOverlappSjekk(kontrollperiodeUtenFom, medlemsperiode))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void harPeriodeSomOverlapperMerEnn1Dag_manglerTomPåBeggePerioder_kasterIllegalArgumentException() {
        Periode kontrollperiodeUtenTom = new Periode(LocalDate.EPOCH, null);
        Periode medlemsperiodeUtenTom = new Periode(LocalDate.EPOCH.plusYears(1), null);

        assertThatThrownBy(() -> new PeriodeOverlappSjekk(kontrollperiodeUtenTom, medlemsperiodeUtenTom))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
