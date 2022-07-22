package no.nav.melosys.service.kontroll.feature.godkjennunntak.kontroll;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.data.UnntaksperiodeKontrollData;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

public final class UnntaksperiodeKontroll {

    private UnntaksperiodeKontroll() {
    }

    static Kontrollfeil periodeOver24MånederOgEnDag(UnntaksperiodeKontrollData kontrollData) {
        return PeriodeRegler.periodeOver2ÅrOgEnDag(kontrollData.periodeFom(), kontrollData.periodeTom()) ?
            new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) :
            null;
    }
}
