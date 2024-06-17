package no.nav.melosys.service.avgift.aarsavregning

import jakarta.ws.rs.NotAllowedException
import jakarta.ws.rs.NotFoundException
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Aarsavregning
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ÅrsavregningService(
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val aarsavregningRepository: AarsavregningRepository,
    private val behandlingsresultatRepository: BehandlingsresultatRepository
) {
    fun beregnTotalbeløpForPeriode(beregnTotalBeløpDto: BeregnTotalBeløpDto): BigDecimal {
        val saksbehandlerIdent = SubjectHandler.getInstance().getUserID()
        return faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode(beregnTotalBeløpDto, saksbehandlerIdent)
    }

    @Transactional(readOnly = true)
    fun hentÅrsavregning(avregningID: Long): Årsavregning {
        val aarsavregning = aarsavregningRepository.findById(avregningID).orElseThrow { IkkeFunnetException("Fant ikke årsavregning $avregningID") }
        val år = aarsavregning.aar
        return Årsavregning(
            år = år,
            tidligereGrunnlag = hentTidligereTrygdeavgiftsgrunnlag(år, aarsavregning.tidligereBehandlingsresultat),
            tidligereAvgift = aarsavregning.tidligereBehandlingsresultat?.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(år) }.orEmpty(),
            nyttGrunnlag = hentNyttTrygdeavgiftsgrunnlag(aarsavregning),
            endeligAvgift = aarsavregning.behandlingsresultat.trygdeavgiftsperioder.toList(),
            tidligereFakturertBeloep = aarsavregning.tidligereFakturertBeloep,
            nyttTotalbeloep = aarsavregning.nyttTotalbeloep,
            tilFaktureringBeloep = aarsavregning.tilFaktureringBeloep
        )
    }

    @Transactional
    fun opprettNyÅrsavregning(behandlingsId: Long, gjelderPeriode: Int): Long {
        val behandlingsresultat = behandlingsresultatRepository.findById(behandlingsId)
        if (behandlingsresultat.isPresent) {
            oppretteÅrsavregning(behandlingsresultat.get(), gjelderPeriode)
            return behandlingsId
        } else {
            throw NotFoundException("Finner ingen tidligere behandlingsresultat")
        }
    }

    @Transactional
    fun oppretteÅrsavregning(behandlingsresultat: Behandlingsresultat, gjelderPeriode: Int) {
        if (aarsavregningRepository.existsAarsavregningByBehandlingAndYear(behandlingsresultat.behandling.id, gjelderPeriode))
            throw NotAllowedException("Du har ikke lov til å ha 2 åpne behandlinger for årsavregning på samme år")

        Aarsavregning().apply {
            aar = gjelderPeriode
            behandlingsresultat.aarsavregning = this
            this.behandlingsresultat = behandlingsresultat
        }.also {
            aarsavregningRepository.save(it)
        }
    }

    private fun hentTidligereTrygdeavgiftsgrunnlag(år: Int, behandlingsresultat: Behandlingsresultat?): Trygdeavgiftsgrunnlag? {
        if (behandlingsresultat == null) return null

        return Trygdeavgiftsgrunnlag(
            medlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.overlapperMedÅr(år) }.map(::MedlemskapsperiodeForAvgift),
            skatteforholdsperioder = behandlingsresultat.hentSkatteforholdTilNorge().filter { it.overlapperMedÅr(år) },
            innteksperioder = behandlingsresultat.hentInntektsperioder().filter { it.overlapperMedÅr(år) }
        )
    }

    private fun hentNyttTrygdeavgiftsgrunnlag(aarsavregning: Aarsavregning): Trygdeavgiftsgrunnlag? {
        val behandlingsresultat = aarsavregning.behandlingsresultat
        if (behandlingsresultat.medlemskapsperioder.isEmpty() && behandlingsresultat.hentSkatteforholdTilNorge().isEmpty() && behandlingsresultat.hentInntektsperioder().isEmpty()) {
            return null
        }
        return Trygdeavgiftsgrunnlag(
            medlemskapsperioder = behandlingsresultat.medlemskapsperioder.map(::MedlemskapsperiodeForAvgift),
            skatteforholdsperioder = behandlingsresultat.hentSkatteforholdTilNorge().toList(),
            innteksperioder = behandlingsresultat.hentInntektsperioder().toList()
        )
    }
}

data class Årsavregning(
    val år: Int,
    val tidligereGrunnlag: Trygdeavgiftsgrunnlag?,
    val tidligereAvgift: List<Trygdeavgiftsperiode>,
    val nyttGrunnlag: Trygdeavgiftsgrunnlag?,
    val endeligAvgift: List<Trygdeavgiftsperiode>,
    val tidligereFakturertBeloep: BigDecimal?,
    val nyttTotalbeloep: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
)

data class Trygdeavgiftsgrunnlag(
    val medlemskapsperioder: List<MedlemskapsperiodeForAvgift>,
    val skatteforholdsperioder: List<SkatteforholdTilNorge>,
    val innteksperioder: List<Inntektsperiode>
)

data class MedlemskapsperiodeForAvgift(
    val fom: LocalDate, val tom: LocalDate, val dekning: Trygdedekninger, val bestemmelse: Folketrygdloven_kap2_bestemmelser
) {
    constructor(medlemskapsperiode: Medlemskapsperiode) : this(
        fom = medlemskapsperiode.fom,
        tom = medlemskapsperiode.tom,
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse
    )
}
