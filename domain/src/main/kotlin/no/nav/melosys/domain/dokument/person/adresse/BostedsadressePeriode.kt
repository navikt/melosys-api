package no.nav.melosys.domain.dokument.person.adresse

import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.LocalDateTime

data class BostedsadressePeriode(
    private var periode: Periode?,
    var endringstidspunkt: LocalDateTime?,
    var bostedsadresse: Bostedsadresse?
) : HarPeriode {
    override fun getPeriode(): Periode? = this.periode
}


