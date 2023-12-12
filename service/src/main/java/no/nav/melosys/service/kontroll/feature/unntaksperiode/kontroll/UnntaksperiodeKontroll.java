package no.nav.melosys.service.kontroll.feature.unntaksperiode.kontroll;

import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import exception.KontrolldataFeilType;
import no.nav.melosys.service.kontroll.feature.unntaksperiode.data.UnntaksperiodeKontrollData;
import no.nav.melosys.service.kontroll.regler.PeriodeRegler;
import no.nav.melosys.service.validering.Kontrollfeil;

public final class UnntaksperiodeKontroll {

    private UnntaksperiodeKontroll() {
    }

    static Kontrollfeil periodeOver24MånederOgEnDag(UnntaksperiodeKontrollData kontrollData) {
        return PeriodeRegler.periodeOver2ÅrOgEnDag(kontrollData.periodeFom(), kontrollData.periodeTom()) ?
            new Kontrollfeil(Kontroll_begrunnelser.PERIODEN_OVER_24_MD, KontrolldataFeilType.FEIL) :
            null;
    }
}
