package no.nav.melosys.service.sak

import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import org.springframework.stereotype.Service

@Service
class ÅrsavregningService (
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer
) {
    fun beregnTotalTrygdeavgiftForPeriode(beregnTotalBeløpDto: BeregnTotalBeløpDto) =
        faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(beregnTotalBeløpDto)
}
