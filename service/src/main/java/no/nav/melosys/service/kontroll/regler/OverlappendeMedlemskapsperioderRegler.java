package no.nav.melosys.service.kontroll.regler;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;

public final class OverlappendeMedlemskapsperioderRegler {

    public static boolean harOverlappendeMedlemsperiodeFraSed(MedlemskapDokument medlemskapDokument,
                                                              ErPeriode kontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status)
                && PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode()));
    }

    public static boolean harOverlappendeMedlemsperiode(MedlemskapDokument medlemskapDokument,
                                                        Lovvalgsperiode kontrollperiode,
                                                        Lovvalgsperiode opprinneligPeriodeTilKontrollperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status)
                && PeriodeRegler.periodeOverlapper(kontrollperiode, medlemsperiode.getPeriode())
                && (kontrollperiode.erNyPeriodeForMedl() || !kontrollperiode.harSammeMedlID(medlemsperiode.id))
                && (opprinneligPeriodeTilKontrollperiode == null || !opprinneligPeriodeTilKontrollperiode.harSammeMedlID(medlemsperiode.id)
            ));
    }
}
