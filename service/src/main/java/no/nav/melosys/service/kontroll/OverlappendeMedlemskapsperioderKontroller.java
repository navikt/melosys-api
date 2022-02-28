package no.nav.melosys.service.kontroll;

import no.nav.melosys.domain.ErPeriode;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.integrasjon.medl.PeriodestatusMedl;

public final class OverlappendeMedlemskapsperioderKontroller {

    private OverlappendeMedlemskapsperioderKontroller() {
    }

    public static boolean harOverlappendeMedlemsperiodeIkkeAvvistIPeriode(MedlemskapDokument medlemskapDokument,
                                                                          ErPeriode periode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> !PeriodestatusMedl.AVST.getKode().equals(medlemsperiode.status)
                && PeriodeKontroller.periodeOverlapper(periode, medlemsperiode.getPeriode()));
    }

    public static boolean harOverlappendeMedlemsperiodeGyldigIPeriode(MedlemskapDokument medlemskapDokument,
                                                                      Lovvalgsperiode lovvalgsperiode) {
        return medlemskapDokument.hentMedlemsperioderHvorKildeIkkeLånekassen().stream().anyMatch(
            medlemsperiode -> PeriodestatusMedl.GYLD.getKode().equals(medlemsperiode.status)
                && PeriodeKontroller.periodeOverlapper(lovvalgsperiode, medlemsperiode.getPeriode())
                && (lovvalgsperiode.getMedlPeriodeID() == null || !lovvalgsperiode.getMedlPeriodeID().equals(
                    medlemsperiode.id)));
    }
}
