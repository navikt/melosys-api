package no.nav.melosys.service.avgift.aarsavregning

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Component
class AvgiftTotalBeregner(
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
) {

    fun hentTotalAvgift(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal? {
        if (trygdeavgiftsperioder.isEmpty()) {
            return null
        }
        val fakturaseriePerioder = trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                startDato = it.periodeFra,
                sluttDato = it.periodeTil,
                enhetsprisPerManed = it.trygdeavgiftsbeløpMd.verdi,
                beskrivelse = "FIXME"
            )
        }
        val saksbehandlerIdent = SubjectHandler.getInstance().getUserID() ?: ThreadLocalAccessInfo.getSaksbehandler()
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(BeregnTotalBeløpDto(fakturaseriePerioder), saksbehandlerIdent)
    }

    fun hentTotalInntekt(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal {
        val fakturaseriePerioder = trygdeavgiftsperioder.map {
            FakturaseriePeriodeDto(
                startDato = it.periodeFra,
                sluttDato = it.periodeTil,
                enhetsprisPerManed = it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi,
                beskrivelse = "FIXME"
            )
        }
        val saksbehandlerIdent = SubjectHandler.getInstance().getUserID() ?: ThreadLocalAccessInfo.getSaksbehandler()
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(BeregnTotalBeløpDto(fakturaseriePerioder), saksbehandlerIdent)
    }
}
