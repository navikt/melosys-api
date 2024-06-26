package no.nav.melosys.service.avgift.aarsavregning

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Aarsavregning
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ÅrsavregningService(
    private val aarsavregningRepository: AarsavregningRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val faktureringskomponentenConsumer: FaktureringskomponentenConsumer,
    private val trygdeavgiftService: TrygdeavgiftService
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
    fun opprettÅrsavregning(behandlingID: Long, gjelderÅr: Int): Long {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        if (aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(behandlingID, gjelderÅr) != 0) {
            throw FunksjonellException("Det finnes en annen åpen årsavregningsbehandling for samme år på saken. \n" +
                "Vurder hvilke behandling du vil fortsette med og avslutt den som ikke er aktuell via behandlingsmenyen.")
        }

        Aarsavregning().apply {
            behandlingsresultat.aarsavregning = this
            aar = gjelderÅr
            this.behandlingsresultat = behandlingsresultat
            tidligereBehandlingsresultat = finnTidligereBehandlingsresultatMedAvgift(behandlingsresultat.behandling.fagsak.saksnummer, gjelderÅr)
        }.also {
            aarsavregningRepository.save(it)
        }
        return behandlingID
    }

    private fun finnTidligereBehandlingsresultatMedAvgift(saksnummer: String, gjelderÅr: Int): Behandlingsresultat? =
        trygdeavgiftService.finnSistFakturerbarTrygdeavgiftsbehandlingForÅr(saksnummer, gjelderÅr)
            ?.let { behandlingsresultatService.hentBehandlingsresultat(it.id) }

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
