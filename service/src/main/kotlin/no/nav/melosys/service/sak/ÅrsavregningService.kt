package no.nav.melosys.service.sak

import no.nav.melosys.domain.avgift.Aarsavregning
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class ÅrsavregningService (
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer
) {
    fun beregnTotalTrygdeavgiftForPeriode(beregnTotalBeløpDto: BeregnTotalBeløpDto): BigDecimal {
        val saksbehandlerIdent = SubjectHandler.getInstance().getUserID()
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(beregnTotalBeløpDto, saksbehandlerIdent)
    }

    fun hentÅrsavregnig(avregningID: Long): Aarsavregning {
        TODO("")
    }
}
