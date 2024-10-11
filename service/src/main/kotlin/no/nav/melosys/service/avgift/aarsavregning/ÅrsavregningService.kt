package no.nav.melosys.service.avgift.aarsavregning

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.FagsakService.UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT
import org.apache.commons.beanutils.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ÅrsavregningService(
    private val aarsavregningRepository: AarsavregningRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val totalBeløpBeregner: TotalBeløpBeregner,
    private val fagsakService: FagsakService,
) {
    fun hentÅrsavregning(aarsavregningId: Long) =
        aarsavregningRepository.findById(aarsavregningId).orElseThrow { IkkeFunnetException("Finner ingen årsavregning for id: $aarsavregningId") }


    @Transactional(readOnly = true)
    fun finnÅrsavregningerPåFagsak(saksnummer: String, aar: Int?, behandlingstype: Behandlingsresultattyper?): List<Årsavregning> {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        return fagsak.behandlinger
            .filter { it.erÅrsavregning() }
            .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
            .filter { behandlingstype == null || it.type == behandlingstype }
            .mapNotNull { it.årsavregning }
            .filter { aar == null || it.aar == aar }
    }

    @Transactional(readOnly = true)
    fun finnÅrsavregning(behandlingID: Long): ÅrsavregningModel? {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        val aarsavregning = behandlingsresultat.årsavregning ?: return null

        return lagÅrsavregningModelFraÅrsavregning(aarsavregning)
    }

    @Transactional(readOnly = true)
    fun finnGjeldendeÅrForÅrsavregning(behandlingID: Long): Int? {
        return finnÅrsavregning(behandlingID)?.år
    }

    @Transactional
    fun opprettÅrsavregning(behandlingID: Long, gjelderÅr: Int): ÅrsavregningModel {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        if (behandlingsresultat.årsavregning != null && behandlingsresultat.årsavregning?.aar == gjelderÅr) {
            throw FunksjonellException("Året $gjelderÅr er allerede lagret på denne årsavregningen")
        }
        if (aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(behandlingID, gjelderÅr) != 0) {
            throw FunksjonellException(
                "Det finnes en annen åpen årsavregningsbehandling for samme år på saken. " +
                    "Vurder hvilke behandling du vil fortsette med og avslutt den uaktuelle behandlingen via behandlingsmeny."
            )
        }

        if (gjelderÅr < LocalDate.now().year - antall_år_tilbake_i_tid) {
            throw FunksjonellException("Årsavregning kan ikke opprettes for år eldre enn 6 år før inneværende år.")
        }

        if (behandlingsresultat.årsavregning != null) {
            behandlingsresultat.årsavregning?.behandlingsresultat = null
            behandlingsresultat.årsavregning = null
            behandlingsresultat.medlemskapsperioder.clear()
            behandlingsresultatService.lagreOgFlush(behandlingsresultat)
        }

        val tidligereBehandlingsresultatMedAvgift = hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
            behandlingsresultat.behandling.fagsak.saksnummer,
            gjelderÅr
        )
        if (tidligereBehandlingsresultatMedAvgift != null) {
            replikerMedlemskapsperioder(
                behandlingsresultat,
                tidligereBehandlingsresultatMedAvgift,
                gjelderÅr
            )
        }

        val årsavregning = Årsavregning().apply {
            behandlingsresultat.årsavregning = this
            aar = gjelderÅr
            this.behandlingsresultat = behandlingsresultat
            tidligereBehandlingsresultat = tidligereBehandlingsresultatMedAvgift
            tidligereFakturertBeloep =
                totalBeløpBeregner.hentTotalAvgift(tidligereBehandlingsresultat?.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(gjelderÅr) }
                    .orEmpty())
        }.also {
            behandlingsresultatService.lagre(behandlingsresultat)
        }

        return lagÅrsavregningModelFraÅrsavregning(årsavregning)
    }

    private fun replikerMedlemskapsperioder(
        behandlingsresultat: Behandlingsresultat,
        tidligereBehandlingsresultat: Behandlingsresultat,
        gjelderÅr: Int
    ) {
        for (medlemskapsperiodeOriginal in tidligereBehandlingsresultat.medlemskapsperioder) {
            if (medlemskapsperiodeOriginal.overlapperMedÅr(gjelderÅr)) {
                val medlemskapsperiodeReplika = BeanUtils.cloneBean(medlemskapsperiodeOriginal) as Medlemskapsperiode
                medlemskapsperiodeReplika.behandlingsresultat = behandlingsresultat
                medlemskapsperiodeReplika.trygdeavgiftsperioder = HashSet()
                medlemskapsperiodeReplika.avkortFomDato(gjelderÅr)
                medlemskapsperiodeReplika.avkortTomDato(gjelderÅr)
                medlemskapsperiodeReplika.id = null
                behandlingsresultat.addMedlemskapsperiode(medlemskapsperiodeReplika)
            }
        }
    }

    private fun lagÅrsavregningModelFraÅrsavregning(årsavregning: Årsavregning): ÅrsavregningModel {
        val år = årsavregning.aar

        return ÅrsavregningModel(
            år = år,
            tidligereGrunnlag = hentTidligereTrygdeavgiftsgrunnlag(år, årsavregning.tidligereBehandlingsresultat),
            tidligereAvgift = årsavregning.tidligereBehandlingsresultat?.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(år) }.orEmpty(),
            nyttGrunnlag = hentNyttTrygdeavgiftsgrunnlag(årsavregning),
            endeligAvgift = årsavregning.behandlingsresultat.trygdeavgiftsperioder.toList(),
            tidligereFakturertBeloep = årsavregning.tidligereFakturertBeloep,
            nyttTotalbeloep = årsavregning.nyttTotalbeloep,
            tilFaktureringBeloep = årsavregning.tilFaktureringBeloep
        )
    }

    fun hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
        saksnummer: String,
        år: Int,
    ): Behandlingsresultat? {
        val fagsak = fagsakService.hentFagsak(saksnummer)

        if (fagsak.status in UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT) {
            return null
        }

        return fagsak.behandlinger
            .filter { it.erAvsluttet() }
            .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
            .filter { it.harInnvilgetMedlemskapsperiodeSomOverlapperMedÅr(år) }
            .sortedBy { it.registrertDato }
            .lastOrNull()
    }

    private fun hentTidligereTrygdeavgiftsgrunnlag(år: Int, behandlingsresultat: Behandlingsresultat?): Trygdeavgiftsgrunnlag? {
        if (behandlingsresultat == null) return null

        return Trygdeavgiftsgrunnlag(
            medlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.overlapperMedÅr(år) }.map(::MedlemskapsperiodeForAvgift),
            skatteforholdsperioder = behandlingsresultat.hentSkatteforholdTilNorge().filter { it.overlapperMedÅr(år) },
            innteksperioder = behandlingsresultat.hentInntektsperioder().filter { it.overlapperMedÅr(år) }
        )
    }

    private fun hentNyttTrygdeavgiftsgrunnlag(årsavregning: Årsavregning): Trygdeavgiftsgrunnlag? {
        val behandlingsresultat = årsavregning.behandlingsresultat
        if (behandlingsresultat.hentSkatteforholdTilNorge()
                .isEmpty() && behandlingsresultat.hentInntektsperioder().isEmpty()
        ) {
            return null
        }
        return Trygdeavgiftsgrunnlag(
            medlemskapsperioder = behandlingsresultat.medlemskapsperioder.map(::MedlemskapsperiodeForAvgift),
            skatteforholdsperioder = behandlingsresultat.hentSkatteforholdTilNorge().toList(),
            innteksperioder = behandlingsresultat.hentInntektsperioder().toList()
        )
    }

    @Transactional
    fun oppdaterTotalbelop(
        behandlingID: Long,
        aarsavregningId: Long,
        tidligereFakturertBeloep: BigDecimal?,
        nyttTotalbeloep: BigDecimal?
    ): ÅrsavregningModel {
        val årsavregning = hentÅrsavregning(aarsavregningId)
        val årsavregningViaBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID).årsavregning
        if (årsavregning != årsavregningViaBehandlingsresultat) {
            throw RuntimeException("Årsavregning med id: $aarsavregningId hører ikke til Behandling med Id: $behandlingID")
        }

        if (tidligereFakturertBeloep != null) årsavregning.tidligereFakturertBeloep = tidligereFakturertBeloep
        if (nyttTotalbeloep != null) årsavregning.nyttTotalbeloep = nyttTotalbeloep
        årsavregning.beregnTilFaktureringsBeloep()

        return lagÅrsavregningModelFraÅrsavregning(årsavregning)
    }

    companion object {
        private val antall_år_tilbake_i_tid = 7  //Fjoråret - 6 år
    }
}

data class ÅrsavregningModel(
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
    val fom: LocalDate,
    val tom: LocalDate,
    val dekning: Trygdedekninger,
    val bestemmelse: Folketrygdloven_kap2_bestemmelser,
    val medlemskapstyper: Medlemskapstyper
) {
    constructor(medlemskapsperiode: Medlemskapsperiode) : this(
        fom = medlemskapsperiode.fom,
        tom = medlemskapsperiode.tom,
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse,
        medlemskapstyper = medlemskapsperiode.medlemskapstype
    )
}
