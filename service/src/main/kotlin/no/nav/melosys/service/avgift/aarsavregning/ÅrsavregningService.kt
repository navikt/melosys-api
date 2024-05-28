package no.nav.melosys.service.avgift.aarsavregning

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ÅrsavregningService(
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val aarsavregningRepository: AarsavregningRepository,
) {
    fun beregnTotalTrygdeavgiftForPeriode(beregnTotalBeløpDto: BeregnTotalBeløpDto): BigDecimal {
        val saksbehandlerIdent = SubjectHandler.getInstance().getUserID()
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(beregnTotalBeløpDto, saksbehandlerIdent)
    }

    @Transactional(readOnly = true)
    fun hentÅrsavregning(avregningID: Long): Årsavregning {
        val aarsavregning = aarsavregningRepository.findById(avregningID).orElseThrow { IkkeFunnetException("Fant ikke årsavregning $avregningID") }
        return Årsavregning(
            aar = aarsavregning.aar,
            tidligereGrunnlag = null,
            tidligereAvgift = listOf(),
            nyttGrunnlag = null, // TODO mer mapping kommer
            endeligAvgift = listOf(),
            tidligereFakturertBeloep = aarsavregning.tidligereFakturertBeloep,
            nyttTotalbeloep = aarsavregning.nyttTotalbeloep,
            tilFaktureringBeloep = aarsavregning.tilFaktureringBeloep
        )
    }
}

data class Årsavregning(
    val aar: Int,
    val tidligereGrunnlag: Trygdeavgiftsgrunnlag?,
    val tidligereAvgift: List<Trygdeavgiftsperiode>,
    val nyttGrunnlag: Trygdeavgiftsgrunnlag?,
    val endeligAvgift: List<Trygdeavgiftsperiode>,
    val tidligereFakturertBeloep: BigDecimal?,
    val nyttTotalbeloep: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
)

data class Trygdeavgiftsgrunnlag(
    val medlemskapsperioder: List<Medlemskapsperiode>,
    val skatteforholdsperioder: List<SkatteforholdTilNorge>,
    val innteksperioder: List<Inntektsperiode>
)
