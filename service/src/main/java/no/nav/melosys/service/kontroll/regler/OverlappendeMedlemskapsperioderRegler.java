package no.nav.melosys.service.kontroll.regler;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.data.MedlemskapsperiodeData;

import java.util.List;
import java.util.Objects;

public final class OverlappendeMedlemskapsperioderRegler {

    public static boolean harOverlappendeMedlemsperiodeFraSed(MedlemskapDokument medlemskapDokument,
                                                              ErPeriode kontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.kode.equals(medlemsperiode.getStatus())
                && PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode()));
    }

    public static boolean harOverlappendeMedlemsperiodeMerEnn1DagFraSed(MedlemskapDokument medlemskapDokument,
                                                                        ErPeriode kontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.kode.equals(medlemsperiode.getStatus())
                && PeriodeRegler.perioderOverlapperMerEnn1Dag(kontrollperiode, medlemsperiode.getPeriode()));
    }

    public static boolean harOverlappendePeriode(MedlemskapDokument medlemskapDokument,
                                                 PeriodeOmLovvalg kontrollperiode,
                                                 Lovvalgsperiode opprinneligPeriodeTilKontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen()
            .stream()
            .filter(medlemsperiode -> !PeriodestatusMedl.AVST.kode.equals(medlemsperiode.getStatus()))
            .anyMatch(medlemsperiode ->
                PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode())
                    && (kontrollperiode.erNyPeriodeForMedl() || kontrollperiode.harForskjelligMedlID(medlemsperiode.getId()))
                    && (opprinneligPeriodeTilKontrollperiode == null || opprinneligPeriodeTilKontrollperiode.harForskjelligMedlID(medlemsperiode.getId())
                ));
    }

    public static boolean harOverlappendePeriode(MedlemskapDokument medlemskapDokument,
                                                 MedlemskapsperiodeData medlemskapsperiodeData) {
        return medlemskapsperiodeData.getNyeMedlemskapsperioder().stream().anyMatch(kontrollperiode ->
            medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream()
                .filter(medlemsperiode -> !PeriodestatusMedl.AVST.kode.equals(medlemsperiode.getStatus()))
                .filter(medlemsperiode -> !medlemskapsperiodeData.medlIdEksistererPåTidligereMedlemskapsperiode(medlemsperiode.getId()))
                .anyMatch(medlemsperiode ->
                    PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode())
                        && (kontrollperiode.getMedlPeriodeID() == null || !Objects.equals(kontrollperiode.getMedlPeriodeID(), medlemsperiode.getId()))
                )
        );
    }

    public static boolean harOverlappendePeriodeMedForskuddsvisFaktureringIAndreFagsaker(
            MedlemskapsperiodeData medlemskapsperiodeData, String gjeldendeSaksnummer) {

        List<Medlemskapsperiode> tidligereMedlemskapsperioderIandreFagsaker = medlemskapsperiodeData
                .getTidligereMedlemskapsperioderForBukerMedAvgift().stream()
                .filter(medlemsperiode ->
                        !medlemsperiode.getBehandlingsresultat().getBehandling().getFagsak().getSaksnummer().equals(gjeldendeSaksnummer))
                .toList();

        return medlemskapsperiodeData.getNyeMedlemskapsperioderMedAvgift().stream()
                .anyMatch(nyMedlemskapsperiode -> harOverlappMedTidligerePerioder(nyMedlemskapsperiode, tidligereMedlemskapsperioderIandreFagsaker));
    }

    private static boolean harOverlappMedTidligerePerioder(
            Medlemskapsperiode nyMedlemskapsPeriode, List<Medlemskapsperiode> tidligereMedlemskapsperioder) {

        return tidligereMedlemskapsperioder.stream()
                .anyMatch(tidligereMedlemskapsperiode -> PeriodeRegler.periodeOverlapper(nyMedlemskapsPeriode, tidligereMedlemskapsperiode));
    }

    public static boolean harOverlappendeUnntaksperiode(MedlemskapDokument medlemskapDokument,
                                                        PeriodeOmLovvalg kontrollperiode,
                                                        Lovvalgsperiode opprinneligPeriodeTilKontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream()
            .filter(Medlemsperiode::erUnntaksperiode)
            .filter(medlemsperiode -> !PeriodestatusMedl.AVST.kode.equals(medlemsperiode.getStatus()))
            .anyMatch(medlemsperiode -> PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode())
                && (kontrollperiode.erNyPeriodeForMedl() || kontrollperiode.harForskjelligMedlID(medlemsperiode.getId()))
                && (opprinneligPeriodeTilKontrollperiode == null || opprinneligPeriodeTilKontrollperiode.harForskjelligMedlID(medlemsperiode.getId())
            ));
    }

    public static boolean harOverlappendeMedlemsperiode(MedlemskapDokument medlemskapDokument,
                                                        PeriodeOmLovvalg kontrollperiode,
                                                        Lovvalgsperiode opprinneligPeriodeTilKontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream()
            .filter(Medlemsperiode::erMedlemskapsperiode)
            .filter(medlemsperiode -> !PeriodestatusMedl.AVST.kode.equals(medlemsperiode.getStatus()))
            .anyMatch(medlemsperiode -> PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode())
                && (kontrollperiode.erNyPeriodeForMedl() || kontrollperiode.harForskjelligMedlID(medlemsperiode.getId()))
                && (opprinneligPeriodeTilKontrollperiode == null || opprinneligPeriodeTilKontrollperiode.harForskjelligMedlID(medlemsperiode.getId())
            ));
    }

    public static boolean harOverlappendePerioderMedUlikSedLovvalgslandOgMedlLovvalgsland(SedDokument sedDokument,
                                                                                          MedlemskapDokument medlemskapDokument) {
        if (medlemskapDokument == null || medlemskapDokument.getMedlemsperiode().isEmpty()) {
            return false;
        }
        var sedLovvalgsperiode = sedDokument.getLovvalgsperiode();
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.kode.equals(medlemsperiode.getStatus())
                && PeriodeRegler.perioderOverlapperMerEnn1Dag(sedLovvalgsperiode, medlemsperiode.getPeriode())
                && !sedDokument.getLovvalgslandKode().getKode().equals(medlemsperiode.hentLandSomIso2()));
    }
}
