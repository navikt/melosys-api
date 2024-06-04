package no.nav.melosys.service.sak

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Aarsavregning
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
            tidligereAvgift = null, // TODO mer mapping kommer
            endeligAvgift = null,
            tidligereFakturertBeloep = aarsavregning.tidligereFakturertBeloep,
            nyttTotalbeloep = aarsavregning.nyttTotalbeloep,
            tilFaktureringBeloep = aarsavregning.tilFaktureringBeloep
        )
    }

    fun oppretteÅrsavregning(behandlingsresultat: Behandlingsresultat, gjelderPeriode: Int) {
        Aarsavregning().apply {
            aar = gjelderPeriode
            this.behandlingsresultat = behandlingsresultat
        }.also {
            aarsavregningRepository.save(it)
        }
    }
}

data class Årsavregning(
    val aar: Int,
    val tidligereAvgift: Trygdeavgift?,
    val endeligAvgift: Trygdeavgift?,
    val tidligereFakturertBeloep: BigDecimal?,
    val nyttTotalbeloep: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
)

data class Trygdeavgift(
    val grunnlag: Trygdeavgiftsgrunnlag,
    val avgiftsperioder: List<Trygdeavgiftsperiode>
)

data class Trygdeavgiftsgrunnlag(
    val medlemskapsperioder: List<Medlemskapsperiode>,
    val skatteforholdsperioder: List<SkatteforholdTilNorge>,
    val innteksperioder: List<Inntektsperiode>
)
