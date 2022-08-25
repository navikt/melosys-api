package no.nav.melosys.service.kontroll.regler;

import java.util.function.Predicate;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.PeriodeOmLovvalg;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OverlappendeMedlemskapsperioderRegler {
    private static final Logger log = LoggerFactory.getLogger(OverlappendeMedlemskapsperioderRegler.class);

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
                                                        PeriodeOmLovvalg opprinneligPeriodeTilKontrollperiode) {

        Predicate<Medlemsperiode> medlemsperiodePredicate = medlemsperiode -> {
            boolean erIkkeAvvist = !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status);
            log.info("[harOverlappendeMedlemsperiode] erIkkeAvvist: {}", erIkkeAvvist);
            boolean periodeOverlapper = PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode());
            log.info("[harOverlappendeMedlemsperiode] periodeOverlapper: {}", periodeOverlapper);
            boolean erNyPeriode = kontrollperiode.erNyPeriodeForMedl() || kontrollperiode.harForskjelligMedlID(medlemsperiode.id);
            log.info("[harOverlappendeMedlemsperiode] erNyPeriode: {}", erNyPeriode);
            boolean erOpprinneligPeriodeForskjellig = opprinneligPeriodeTilKontrollperiode == null || opprinneligPeriodeTilKontrollperiode.harForskjelligMedlID(medlemsperiode.id);
            log.info("[harOverlappendeMedlemsperiode] erOpprinneligPeriodeForskjellig: {}", erOpprinneligPeriodeForskjellig);
            boolean harOverlappendeMedlemsperiode = erIkkeAvvist
                && periodeOverlapper
                && erNyPeriode
                && erOpprinneligPeriodeForskjellig;
            log.info("[harOverlappendeMedlemsperiode] harOverlappendeMedlemsperiode: {}", harOverlappendeMedlemsperiode);
            return harOverlappendeMedlemsperiode;
        };
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen()
            .stream()
            .anyMatch(medlemsperiodePredicate);
    }
}
