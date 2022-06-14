package no.nav.melosys.service.kontroll.feature.godkjennunntak.kontroll;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.service.kontroll.feature.godkjennunntak.data.GodkjennUnntakKontrollData;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

public final class GodkjennUnntakKontroll {

    private GodkjennUnntakKontroll() {
    }

    static Kontrollfeil periodeOver24MånederOgEnDag(GodkjennUnntakKontrollData kontrollData) {
        LocalDate fom = kontrollData.lovvalgsperiode().getFom();
        LocalDate tom = kontrollData.lovvalgsperiode().getTom();
        return PeriodeRegler.periodeOver2ÅrOgEnDag(fom, tom) ?
            new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD) :
            null;
    }
}
