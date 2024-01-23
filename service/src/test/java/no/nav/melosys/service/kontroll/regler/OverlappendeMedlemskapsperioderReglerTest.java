package no.nav.melosys.service.kontroll.regler;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.MedlemskapsperiodeData;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.service.kontroll.regler.OverlappendeMedlemskapsperioderRegler.*;
import static org.junit.jupiter.api.Assertions.*;

class OverlappendeMedlemskapsperioderReglerTest {

    @Test
    void overlappendePeriode_tidligerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertFalse(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(2), LocalDate.EPOCH.minusYears(1)),
            null)
        );
    }

    @Test
    void overlappendePeriode_senerePeriodeIkkeOverlappendePerioder_ingenTreff() {
        assertFalse(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(3), LocalDate.EPOCH.plusYears(5L)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_1() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }

    @Test
    void overlappendeUnntakPeriode_overlappendePeriode_registrerTreff_1() {
        assertTrue(harOverlappendeUnntaksperiode(
            lagMedlemskapsDokument("SWE"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsPeriode_overlappendePeriode_registrerTreff_1() {
        assertTrue(harOverlappendeMedlemsperiode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_2() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(5)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_3() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(5)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_4() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1), LocalDate.EPOCH.plusYears(1)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_5() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_6() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(3)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriode_registrerTreff_7() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeOgTomErNull_registrerTreff() {
        assertTrue(harOverlappendePeriode(
            lagMedlemskapsDokument("NOR"),
            lagLovvalgsPeriode(LocalDate.EPOCH.minusYears(1),
                null), null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeAvvistPeriode_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.AVST.getKode();
        assertFalse(harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeUavklartPeriode_registrerTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        assertTrue(harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendeMedlemsperiodeFraSed_overlappendePeriodeErUAVKL_registrerTreff() {
        MedlemskapDokument medlemskapDokument = lagUavklartMedlemskapsDokument();
        assertTrue(harOverlappendeMedlemsperiodeFraSed(
            medlemskapDokument, new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)))
        );
    }


    @Test
    void harOverlappendeMedlemsperiodeMerEnn1DagFraSed_inklusivOverlappendePeriode_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagUavklartMedlemskapsDokument();
        Lovvalgsperiode kontrollperiode = lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));


        boolean erTattIKontroll = harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskapDokument, kontrollperiode);


        assertFalse(erTattIKontroll);
    }

    @Test
    void harOverlappendeMedlemsperiodeMerEnn1DagFraSed_inklusivOverlappendePeriode_medEnDagOver_registrerTreff() {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        medlemsperiode.periode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2).plusDays(1));
        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);

        Lovvalgsperiode kontrollperiode = lagLovvalgsPeriode(LocalDate.EPOCH.plusYears(2), LocalDate.EPOCH.plusYears(4));


        boolean erTattIKontroll = harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskapDokument, kontrollperiode);


        assertTrue(erTattIKontroll);
    }

    @Test
    void harOverlappendeMedlemsperiodeMerEnn1DagFraSed_tidligerePeriodeOverlapperMedEnDag_under2År_ingenTreff() {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemsperiode.periode = new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 5, 1));
        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);

        Lovvalgsperiode kontrollperiode = lagLovvalgsPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));


        boolean erTattIKontroll = harOverlappendeMedlemsperiodeMerEnn1DagFraSed(medlemskapDokument, kontrollperiode);


        assertFalse(erTattIKontroll);
    }

    @Test
    void overlappendePeriode_kildeLånekassen_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.kilde = "LAANEKASSEN";
        assertFalse(harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeNyVurderingLovvalgsperiodeHarSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        lovvalgsperiode.setMedlPeriodeID(123L);
        assertFalse(harOverlappendePeriode(
            medlemskapDokument, lovvalgsperiode, null)
        );
    }

    @Test
    void overlappendePeriode_overlappendePeriodeNyVurderingOpprinneligPeriodeHarSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        Lovvalgsperiode opprinneligLovvalgsperiode = lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        opprinneligLovvalgsperiode.setMedlPeriodeID(123L);
        assertFalse(harOverlappendePeriode(
            medlemskapDokument,
            lagLovvalgsPeriode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            opprinneligLovvalgsperiode)
        );
    }

    @Test
    void harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland_medOverlappendePeriode_likLand_forventerIngenFeil() {

        SedDokument sedDokument = new SedDokument();
        sedDokument.setLovvalgslandKode(Landkoder.SE);
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.of(2022, 1, 15), LocalDate.of(2022, 3, 1)));

        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();
        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemsperiode.periode = new Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 2, 1));
        medlemsperiode.land = "SWE";
        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);


        boolean harFeil = harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland(sedDokument, medlemskapDokument);


        assertFalse(harFeil);
    }


    @Test
    void harOverlappendePeriode_kildeLånekassen_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.kilde = "LAANEKASSEN";
        List<Medlemskapsperiode> kontrollMedlemskapsperioder = lagMedlemskapsperiodeListe(
            lagMedlemskapsperiode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2))
        );
        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            new MedlemskapsperiodeData(kontrollMedlemskapsperioder, Collections.emptyList())
        ));
    }

    @Test
    void harOverlappendePeriode_overlappendePeriodeMedSammeMedlPeriodeID_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.id = 123L;
        List<Medlemskapsperiode> kontrollMedlemskapsperioder = lagMedlemskapsperiodeListe(
            lagMedlemskapsperiodeMedID(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2), 123L)
        );
        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            new MedlemskapsperiodeData(kontrollMedlemskapsperioder, Collections.emptyList())
        ));
    }

    @Test
    void harOverlappendePeriode_flerePerioderIngenOverlapp_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");

        List<Medlemskapsperiode> kontrollMedlemskapsperioder = lagMedlemskapsperiodeListe(
            lagMedlemskapsperiode(LocalDate.EPOCH.plusYears(3), LocalDate.EPOCH.plusYears(5)),
            lagMedlemskapsperiode(LocalDate.EPOCH.plusYears(6), LocalDate.EPOCH.plusYears(7))
        );

        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            new MedlemskapsperiodeData(kontrollMedlemskapsperioder, Collections.emptyList())
        ));
    }

    @Test
    void harOverlappendePeriode_flerePerioderMedOverlapp_treff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        List<Medlemskapsperiode> kontrollMedlemskapsperioder = lagMedlemskapsperiodeListe(
            lagMedlemskapsperiode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2)),
            lagMedlemskapsperiode(LocalDate.EPOCH.plusYears(1), LocalDate.EPOCH.plusYears(3))
        );

        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            new MedlemskapsperiodeData(kontrollMedlemskapsperioder, Collections.emptyList())
        ));
    }

    @Test
    void harOverlappendePeriode_periodeMedAvstStatus_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        medlemskapDokument.medlemsperiode.get(0).status = "AVST";
        Medlemskapsperiode medlemskapsperiode = lagMedlemskapsperiode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        List<Medlemskapsperiode> kontrollMedlemskapsperioder = lagMedlemskapsperiodeListe(medlemskapsperiode);

        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            new MedlemskapsperiodeData(kontrollMedlemskapsperioder, Collections.emptyList())
        ));
    }

    @Test
    void harOverlappendePeriode_overlappendePeriodeMedUlikMedlPeriodeID_treff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        List<Medlemskapsperiode> kontrollMedlemskapsperioder = lagMedlemskapsperiodeListe(
            lagMedlemskapsperiodeMedID(LocalDate.EPOCH.plusMonths(6), LocalDate.EPOCH.plusYears(3), 124L) // Different ID
        );

        assertTrue(OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            new MedlemskapsperiodeData(kontrollMedlemskapsperioder, Collections.emptyList())
        ));
    }

    @Test
    void harOverlappendePeriode_nullKontrollListe_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");

        //noinspection DataFlowIssue
        assertThrows(NullPointerException.class, () ->
            OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
                medlemskapDokument,
                null)
        );
    }

    @Test
    void harOverlappendePeriode_tomKontrollListe_ingenTreff() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        List<Medlemskapsperiode> kontrollMedlemskapsperioder = Collections.emptyList();

        assertFalse(OverlappendeMedlemskapsperioderRegler.harOverlappendePeriode(
            medlemskapDokument,
            new MedlemskapsperiodeData(kontrollMedlemskapsperioder, Collections.emptyList())
        ));
    }


    private static MedlemskapDokument lagMedlemskapsDokument(String land) {
        MedlemskapDokument medlemskapDokument = new MedlemskapDokument();

        Medlemsperiode medlemsperiode = new Medlemsperiode();
        medlemsperiode.id = 1L;
        medlemsperiode.status = PeriodestatusMedl.GYLD.getKode();
        medlemsperiode.periode = new Periode(LocalDate.EPOCH, LocalDate.EPOCH.plusYears(2));
        medlemsperiode.land = land;

        medlemskapDokument.medlemsperiode = Collections.singletonList(medlemsperiode);
        return medlemskapDokument;
    }

    private MedlemskapDokument lagUavklartMedlemskapsDokument() {
        MedlemskapDokument medlemskapDokument = lagMedlemskapsDokument("NOR");
        Medlemsperiode medlemsperiode = medlemskapDokument.getMedlemsperiode().get(0);
        medlemsperiode.status = PeriodestatusMedl.UAVK.getKode();
        return medlemskapDokument;
    }


    private static Lovvalgsperiode lagLovvalgsPeriode(LocalDate fom, LocalDate tom) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setFom(fom);
        lovvalgsperiode.setTom(tom);
        return lovvalgsperiode;
    }

    private Medlemskapsperiode lagMedlemskapsperiode(LocalDate fraOgMed, LocalDate tilOgMed) {
        return lagMedlemskapsperiodeMedID(fraOgMed, tilOgMed, null);
    }

    private Medlemskapsperiode lagMedlemskapsperiodeMedID(LocalDate fraOgMed, LocalDate tilOgMed, Long medlId) {
        Medlemskapsperiode medlemskapsperiode = new Medlemskapsperiode();
        medlemskapsperiode.setId(1L);
        medlemskapsperiode.setFom(fraOgMed);
        medlemskapsperiode.setTom(tilOgMed);
        medlemskapsperiode.setArbeidsland("BR");
        medlemskapsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.DELVIS_INNVILGET);
        medlemskapsperiode.setMedlemskapstype(Medlemskapstyper.FRIVILLIG);
        medlemskapsperiode.setTrygdedekning(Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON);
        medlemskapsperiode.setMedlPeriodeID(medlId);
        return medlemskapsperiode;
    }

    private List<Medlemskapsperiode> lagMedlemskapsperiodeListe(Medlemskapsperiode... medlemskapsperioder) {
        return Arrays.asList(medlemskapsperioder);
    }
}
