package no.nav.melosys.service.sak

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

    fun hentPerioderForAarsavregningForBehandling(behandlingsId: Long, år: Int): ÅrsavregningDto {
        //TODO vi må filtrere også på sluttdato for alle andre perioder enn trygdeavgiftsPerioder
        val medlemAvFolketrygden = behandlingsresultatService.hentBehandlingsresultat(behandlingsId).medlemAvFolketrygden
        val fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift
        return ÅrsavregningDto.av(trygdeavgiftsPerioder = fastsattTrygdeavgift.trygdeavgiftsperioder.filter { it.periodeFra.year == år },
            skatteforholdsperioder = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.filter { it.fomDato.year == år },
            inntektskilder = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.filter { it.fomDato.year == år },
            medlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder.filter { it.fom.year == år })
    }
}
