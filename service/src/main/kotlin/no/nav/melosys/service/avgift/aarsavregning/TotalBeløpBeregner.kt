package no.nav.melosys.service.avgift.aarsavregning

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class TotalBeløpBeregner(
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
            )
        }
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(BeregnTotalBeløpDto(fakturaseriePerioder), saksbehandlerIdent)
    }

    fun hentTotalInntekt(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal {
        val fakturaseriePerioder = trygdeavgiftsperioder
            .filter { it.grunnlagInntekstperiode != null }
            .map {
                FakturaseriePeriodeDto(
                    startDato = it.periodeFra,
                    sluttDato = it.periodeTil,
                    enhetsprisPerManed = it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi
                )
            }
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(BeregnTotalBeløpDto(fakturaseriePerioder), saksbehandlerIdent)
    }

    fun hentTotalInntektForInntektkilde(inntektsperiode: InntektsperioderForAvgift): BigDecimal {
        val fakturaseriePerioder = listOf(
            FakturaseriePeriodeDto(
                startDato = inntektsperiode.fom,
                sluttDato = inntektsperiode.tom,
                enhetsprisPerManed = inntektsperiode.avgiftspliktigInntektMnd.verdi
            )
        )
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(BeregnTotalBeløpDto(fakturaseriePerioder), saksbehandlerIdent)
    }

    private val saksbehandlerIdent
        get() = SubjectHandler.getInstance().getUserID() ?: ThreadLocalAccessInfo.getSaksbehandler() ?: SubjectHandler.SYSTEMBRUKER

}
