package no.nav.melosys.domain.dokument.person.adresse

import no.nav.melosys.domain.HarPeriode
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.LocalDateTime

data class PostadressePeriode(
    private val periode: Periode,
    val endringstidspunkt: LocalDateTime,
    val postadresse: UstrukturertAdresse
) : HarPeriode {
    override fun getPeriode(): Periode = this.periode
}

