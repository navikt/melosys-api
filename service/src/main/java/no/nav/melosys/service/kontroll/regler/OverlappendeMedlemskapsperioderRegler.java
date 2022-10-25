package no.nav.melosys.service.kontroll.regler;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;

public final class OverlappendeMedlemskapsperioderRegler {

    public static boolean harOverlappendeMedlemsperiodeFraSed(MedlemskapDokument medlemskapDokument,
                                                              ErPeriode kontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status)
                && PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode()));
    }

    public static boolean harOverlappendeMedlemsperiodeMerEnn1DagFraSed(MedlemskapDokument medlemskapDokument,
                                                                        ErPeriode kontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status)
                && PeriodeRegler.perioderOverlapperMerEnn1Dag(kontrollperiode, medlemsperiode.getPeriode()));
    }

    public static boolean harOverlappendeMedlemsperiode(MedlemskapDokument medlemskapDokument,
                                                        PeriodeOmLovvalg kontrollperiode,
                                                        Lovvalgsperiode opprinneligPeriodeTilKontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status)
                && PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode())
                && (kontrollperiode.erNyPeriodeForMedl() || kontrollperiode.harForskjelligMedlID(medlemsperiode.id))
                && (opprinneligPeriodeTilKontrollperiode == null || opprinneligPeriodeTilKontrollperiode.harForskjelligMedlID(medlemsperiode.id)
            ));
    }

    public static boolean harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland(SedDokument sedDokument,
                                                                                          MedlemskapDokument medlemskapDokument) {
        if (medlemskapDokument == null || medlemskapDokument.getMedlemsperiode().isEmpty()) {
            return false;
        }

        var sedLovvalgsperiode = sedDokument.getLovvalgsperiode();
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status)
                && PeriodeRegler.perioderOverlapperMerEnn1Dag(sedLovvalgsperiode, medlemsperiode.getPeriode())
                && !sedDokument.getLovvalgslandKode().getKode().equals(medlemsperiode.getLand()));
    }
}
