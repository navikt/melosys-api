package no.nav.melosys.service.kontroll.feature.godkjennunntak.kontroll;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.data.GodkjennUnntakKontrollData;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

public final class GodkjennUnntakKontroll {

    private GodkjennUnntakKontroll() {
    }

    static Kontrollfeil periodeOver24MånederOgEnDag(GodkjennUnntakKontrollData kontrollData) {
        return PeriodeRegler.periodeOver2ÅrOgEnDag(kontrollData.periodeFom(), kontrollData.periodeTom()) ?
            new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) :
            null;
    }
}
