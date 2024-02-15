package no.nav.melosys.service.ftrl

import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

object GyldigeInnvilgelsesResultat {

    fun hentInnvilgelsesResultat(behandlingstype: Behandlingstyper): List<InnvilgelsesResultat> {
        if (behandlingstype === Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT) {
            return listOf(InnvilgelsesResultat.INNVILGET, InnvilgelsesResultat.AVSLAATT, InnvilgelsesResultat.OPPHØRT)
        }
        return listOf(InnvilgelsesResultat.INNVILGET, InnvilgelsesResultat.AVSLAATT)
    }
}
