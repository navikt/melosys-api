package no.nav.melosys.service.avgift.aarsavregning

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.apache.commons.beanutils.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ÅrsavregningService(
    private val aarsavregningRepository: AarsavregningRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val trygdeavgiftService: TrygdeavgiftService,
    private val trygdeavgiftTotalBeregner: TrygdeavgiftTotalBeregner
) {
    private val MAX_ANTALL_ÅR_TILBAKE_I_TID = 7 //Fjoråret - 6 år

    @Transactional(readOnly = true)
    fun finnÅrsavregning(behandlingID: Long): ÅrsavregningModel? {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val aarsavregning = behandlingsresultat.årsavregning ?: return null

        return ÅrsavregningModel.lagÅrsavregningModelFraÅrsavregning(
            aarsavregning,
            hentTidligereTrygdeavgiftsgrunnlag(aarsavregning.aar, aarsavregning.tidligereBehandlingsresultat),
            hentNyttTrygdeavgiftsgrunnlag(aarsavregning),
            erFørstegangsÅrsavregning(behandlingID)
        )
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

        if (gjelderÅr < LocalDate.now().year - MAX_ANTALL_ÅR_TILBAKE_I_TID) {
            throw FunksjonellException("Årsavregning kan ikke opprettes for år eldre enn 6 år før inneværende år.")
        }

        if (behandlingsresultat.årsavregning != null) {
            behandlingsresultat.årsavregning?.behandlingsresultat = null
            behandlingsresultat.årsavregning = null;
            behandlingsresultat.medlemskapsperioder.clear()
            behandlingsresultatService.lagreOgFlush(behandlingsresultat)
        }

        val tidligereBehandlingsresultatMedAvgift = finnTidligereBehandlingsresultatMedAvgift(behandlingsresultat, gjelderÅr)
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
                trygdeavgiftTotalBeregner.hentTotalAvgift(tidligereBehandlingsresultat?.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(gjelderÅr) }
                    .orEmpty())
        }.also {
            behandlingsresultatService.lagre(behandlingsresultat)
        }

        return ÅrsavregningModel.lagÅrsavregningModelFraÅrsavregning(
            årsavregning,
            hentTidligereTrygdeavgiftsgrunnlag(årsavregning.aar, årsavregning.tidligereBehandlingsresultat),
            hentNyttTrygdeavgiftsgrunnlag(årsavregning),
            erFørstegangsÅrsavregning(behandlingID)
        )
    }

    @Transactional
    fun oppdaterTotalbelop(behandlingID: Long, tidligereFakturertBeloep: BigDecimal?, nyttTotalbeloep: BigDecimal?): ÅrsavregningModel {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        val aarsavregning =
            behandlingsresultat.årsavregning ?: throw RuntimeException("Det eksisterer ikke årsavregning for behandling med id: $behandlingID")
        if (tidligereFakturertBeloep != null) aarsavregning.tidligereFakturertBeloep = tidligereFakturertBeloep
        if (nyttTotalbeloep != null) aarsavregning.nyttTotalbeloep = nyttTotalbeloep
        aarsavregning.beregnTilFaktureringsBeloep()

        return ÅrsavregningModel.lagÅrsavregningModelFraÅrsavregning(
            aarsavregning,
            hentTidligereTrygdeavgiftsgrunnlag(aarsavregning.aar, aarsavregning.tidligereBehandlingsresultat),
            hentNyttTrygdeavgiftsgrunnlag(aarsavregning),
            erFørstegangsÅrsavregning(behandlingID)
        )
    }

    private fun erFørstegangsÅrsavregning(behandlingID: Long) =
        aarsavregningRepository.finnAndreFerdigbehandledeÅrsavregningerPåFagsak(behandlingID) == 0

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

    private fun finnTidligereBehandlingsresultatMedAvgift(behandlingsresultat: Behandlingsresultat, gjelderÅr: Int): Behandlingsresultat? {
        val saksnummer = behandlingsresultat.behandling.fagsak.saksnummer
        val behandling = trygdeavgiftService.finnSistFakturerbarTrygdeavgiftsbehandlingForÅr(saksnummer, gjelderÅr) ?: return null
        return if (behandlingsresultat.behandling == behandling) {
            null
        } else
            behandlingsresultatService.hentBehandlingsresultat(behandling.id)
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
}
