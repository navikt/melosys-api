package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftClient
import no.nav.melosys.integrasjon.trygdeavgift.dto.MinstebeløpResponse
import org.springframework.stereotype.Service

@Service
class MinstebeløpService(
    private val trygdeavgiftClient: TrygdeavgiftClient
) {
    fun finnMinstebeløp(perioder: Collection<Trygdeavgiftsperiode>): MinstebeløpResponse? =
        perioder.filter { it.beregningsregel != Avgiftsberegningsregel.ORDINÆR }.maxByOrNull { it.periodeTil }
            ?.let { trygdeavgiftClient.hentMinstebeløp(it.periodeTil.year) }
}


