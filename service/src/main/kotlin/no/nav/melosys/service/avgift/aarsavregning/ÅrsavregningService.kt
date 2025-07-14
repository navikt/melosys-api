package no.nav.melosys.service.avgift.aarsavregning

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalbeløpBeregner
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.FagsakService.UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT
import org.apache.commons.beanutils.BeanUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Service
class ÅrsavregningService(
    private val aarsavregningRepository: AarsavregningRepository,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val fagsakService: FagsakService,
    private val trygdeavgiftService: TrygdeavgiftService,
) {
    fun hentÅrsavregning(aarsavregningId: Long): Årsavregning =
        aarsavregningRepository.findById(aarsavregningId).orElseThrow { IkkeFunnetException("Finner ingen årsavregning for id: $aarsavregningId") }

    @Transactional(readOnly = true)
    fun finnÅrsavregningerPåFagsak(saksnummer: String, aar: Int?, behandlingsresultattype: Behandlingsresultattyper?): List<Årsavregning> {
        val fagsak = fagsakService.hentFagsak(saksnummer)
        return fagsak.behandlinger
            .filter { it.erÅrsavregning() }
            .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
            .filter { behandlingsresultattype == null || it.type == behandlingsresultattype }
            .mapNotNull { it.årsavregning }
            .filter { aar == null || it.aar == aar }
    }

    @Transactional(readOnly = true)
    fun finnÅrsavregningForBehandling(behandlingID: Long): ÅrsavregningModel? {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        val aarsavregning = behandlingsresultat.årsavregning ?: return null

        return lagÅrsavregningModelFraÅrsavregning(aarsavregning)
    }

    @Transactional(readOnly = true)
    fun finnGjeldendeÅrForÅrsavregning(behandlingID: Long): Int? {
        return finnÅrsavregningForBehandling(behandlingID)?.år
    }

    /**
     * Resetter eksisterende årsavregning dersom behandlingsresultatet er IKKE_FASTSATT.
     * Dette resetter all data saksbehandler har lagt inn på årsavregningen, og oppdaterer grunnlag
     * til siste innvilgede medlemskapsperiode (med avgiftsgrunnlag) for det aktuelle året.
     */
    @Transactional
    fun resetEksisterendeÅrsavregning(behandlingID: Long): ÅrsavregningModel? {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        val eksisterendeÅrsavregning = behandlingsresultat.årsavregning
            ?: throw FunksjonellException("Ingen eksisterende årsavregning funnet på behandlingsresultat=$behandlingID")

        if (behandlingsresultat.type != Behandlingsresultattyper.IKKE_FASTSATT) {
            throw FunksjonellException("Kan ikke oppdatere årsavregning for behandlingsresultat=$behandlingID med type ${behandlingsresultat.type}")
        }

        if (eksisterendeÅrsavregning.aar == null) {
            return null
        }

        return opprettEllerOppdaterÅrsavregning(behandlingsresultat, eksisterendeÅrsavregning.aar)
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
                    "Vurder hvilken behandling du vil fortsette med og avslutt den som ikke er aktuell via behandlingsmenyen."
            )
        }

        if (gjelderÅr < LocalDate.now().year - ANTALL_ÅR_TILBAKE_I_TID) {
            throw FunksjonellException("Årsavregning kan ikke opprettes for år eldre enn 6 år før inneværende år.")
        }

        return opprettEllerOppdaterÅrsavregning(behandlingsresultat, gjelderÅr)
    }

    private fun opprettEllerOppdaterÅrsavregning(
        behandlingsresultat: Behandlingsresultat,
        gjelderÅr: Int
    ): ÅrsavregningModel {
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

        val sisteÅrsavregning = hentSisteÅrsavregning(behandlingsresultat.behandling.fagsak.saksnummer, gjelderÅr)

        val årsavregning = Årsavregning().apply {
            behandlingsresultat.årsavregning = this
            aar = gjelderÅr
            this.behandlingsresultat = behandlingsresultat
            tidligereBehandlingsresultat = tidligereBehandlingsresultatMedAvgift
            tidligereFakturertBeloep =
                sisteÅrsavregning?.manueltAvgiftBeloep
                    ?: TotalbeløpBeregner.hentTotalavgift(tidligereBehandlingsresultat?.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(gjelderÅr) }
                        .orEmpty())
            endeligAvgiftValg = sisteÅrsavregning?.endeligAvgiftValg ?: EndeligAvgiftValg.OPPLYSNINGER_ENDRET
            harTrygdeavgiftFraAvgiftssystemet = sisteÅrsavregning?.let {
                it.harTrygdeavgiftFraAvgiftssystemet ?: true
            }
            trygdeavgiftFraAvgiftssystemet = sisteÅrsavregning?.trygdeavgiftFraAvgiftssystemet
            manueltAvgiftBeloep = sisteÅrsavregning?.manueltAvgiftBeloep
        }.let { årsavregning ->
            behandlingsresultatService.lagre(årsavregning.behandlingsresultat).årsavregning
        }

        return lagÅrsavregningModelFraÅrsavregning(årsavregning)
    }

    fun hentSisteÅrsavregning(saksnummer: String, år: Int, førVedtaksdato: Instant? = null): Årsavregning? {
        val fagsak = fagsakService.hentFagsak(saksnummer)

        if (fagsak.status in UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT) {
            return null
        }

        @Suppress("SimplifiableCallChain") // Det blir ikke riktig med hva IntelliJ foreslår her
        val behandlingsresultat = fagsak.behandlinger
            .filter { it.erAvsluttet() }
            .filter { it.erÅrsavregning() }
            .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
            .filter { it.harInnvilgetMedlemskapsperiodeSomOverlapperMedÅr(år) || harManueltSattAvgift(it, år) }
            .filter { førVedtaksdato == null || it.vedtakMetadata.vedtaksdato < førVedtaksdato}
            .sortedBy { it.vedtakMetadata.vedtaksdato }
            .lastOrNull()

        return behandlingsresultat?.årsavregning
    }

    @Transactional
    fun oppdaterHarSkjoennsfastsattInntektsgrunnlag(
        behandlingID: Long,
        harSkjoennsfastsattInntektsgrunnlag: Boolean
    ): ÅrsavregningModel {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val årsavregning =
            behandlingsresultat.årsavregning ?: throw FunksjonellException("Ingen årsavregning funnet for behandling med id: $behandlingID")
        årsavregning.harSkjoennsfastsattInntektsgrunnlag = harSkjoennsfastsattInntektsgrunnlag
        return lagÅrsavregningModelFraÅrsavregning(årsavregning)
    }

    @Transactional
    fun oppdaterHarTrygdeavgiftFraAvgiftssystemet(
        behandlingID: Long,
        harTrygdeavgiftFraAvgiftssystemet: Boolean
    ): ÅrsavregningModel {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val årsavregning =
            behandlingsresultat.årsavregning ?: throw FunksjonellException("Ingen årsavregning funnet for behandling med id: $behandlingID")
        årsavregning.harTrygdeavgiftFraAvgiftssystemet = harTrygdeavgiftFraAvgiftssystemet
        årsavregning.tilFaktureringBeloep = null
        årsavregning.trygdeavgiftFraAvgiftssystemet = null
        årsavregning.endeligAvgiftValg = EndeligAvgiftValg.OPPLYSNINGER_ENDRET
        årsavregning.manueltAvgiftBeloep = null

        if (!harTrygdeavgiftFraAvgiftssystemet) {
            behandlingsresultat.clearMedlemskapsperioder()

            if (årsavregning.tidligereBehandlingsresultat !== null && årsavregning.tidligereBehandlingsresultat.medlemskapsperioder !== null) {
                replikerMedlemskapsperioder(
                    behandlingsresultat,
                    årsavregning.tidligereBehandlingsresultat,
                    årsavregning.aar
                )
            }
        }

        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
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

        val vedtaksDato =  årsavregning.behandlingsresultat?.vedtakMetadata?.vedtaksdato

        val sisteÅrsavregning = hentSisteÅrsavregning(årsavregning.behandlingsresultat.behandling.fagsak.saksnummer, år, vedtaksDato)

        return ÅrsavregningModel(
            årsavregningID = årsavregning.id,
            år = år,
            tidligereGrunnlag = hentTidligereTrygdeavgiftsgrunnlag(år, årsavregning.tidligereBehandlingsresultat),
            tidligereAvgift = årsavregning.tidligereBehandlingsresultat?.trygdeavgiftsperioder?.filter { it.overlapperMedÅr(år) }.orEmpty(),
            nyttGrunnlag = hentNyttTrygdeavgiftsgrunnlag(årsavregning),
            endeligAvgift = årsavregning.behandlingsresultat.trygdeavgiftsperioder.toList(),
            tidligereFakturertBeloep = årsavregning.tidligereFakturertBeloep,
            beregnetAvgiftBelop = årsavregning.beregnetAvgiftBelop,
            tilFaktureringBeloep = årsavregning.tilFaktureringBeloep,
            harTrygdeavgiftFraAvgiftssystemet = årsavregning.harTrygdeavgiftFraAvgiftssystemet,
            trygdeavgiftFraAvgiftssystemet = årsavregning.trygdeavgiftFraAvgiftssystemet,
            endeligAvgiftValg = årsavregning.endeligAvgiftValg,
            manueltAvgiftBeloep = årsavregning.manueltAvgiftBeloep,
            tidligereTrygdeavgiftFraAvgiftssystemet = sisteÅrsavregning?.trygdeavgiftFraAvgiftssystemet,
            tidligereÅrsavregningmanueltAvgiftBeloep = sisteÅrsavregning?.manueltAvgiftBeloep,
            harSkjoennsfastsattInntektsgrunnlag = årsavregning.harSkjoennsfastsattInntektsgrunnlag
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

        val behandlingsresultattyper = listOf(
            Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT,
            Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
            Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        )

        @Suppress("SimplifiableCallChain") // Det blir ikke riktig med hva IntelliJ foreslår her
        return fagsak.behandlinger
            .filter { it.erAvsluttet() }
            .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
            .filter { it.type in behandlingsresultattyper }
            .filter { it.harInnvilgetMedlemskapsperiodeSomOverlapperMedÅr(år) || harManueltSattAvgift(it, år) }

            .sortedBy { it.registrertDato }
            .lastOrNull()
    }

    private fun harManueltSattAvgift(it: Behandlingsresultat, år: Int) =
        it.årsavregning != null && it.årsavregning.manueltAvgiftBeloep != null && it.årsavregning.aar == år

    private fun hentTidligereTrygdeavgiftsgrunnlag(år: Int, behandlingsresultat: Behandlingsresultat?): Trygdeavgiftsgrunnlag? {
        if (behandlingsresultat == null) return null

        return Trygdeavgiftsgrunnlag(
            medlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.overlapperMedÅr(år) && it.erInnvilget() }
                .map { MedlemskapsperiodeForAvgift(år, it) },
            skatteforholdsperioder = behandlingsresultat.hentSkatteforholdTilNorge().filter { it.overlapperMedÅr(år) }
                .map { SkatteforholdTilNorgeForAvgift(år, it) },
            innteksperioder = behandlingsresultat.hentInntektsperioder().filter { it.overlapperMedÅr(år) }.map { InntektsperioderForAvgift(år, it) }
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
            skatteforholdsperioder = behandlingsresultat.hentSkatteforholdTilNorge().map(::SkatteforholdTilNorgeForAvgift),
            innteksperioder = behandlingsresultat.hentInntektsperioder().map(::InntektsperioderForAvgift)
        )
    }

    @Transactional
    fun oppdater(
        behandlingID: Long,
        aarsavregningId: Long,
        beregnetAvgiftBelop: BigDecimal?,
        trygdeavgiftFraAvgiftssystemet: BigDecimal? = null,
        endeligAvgift: EndeligAvgiftValg? = null,
        manueltAvgiftBeloep: BigDecimal? = null,
    ): ÅrsavregningModel {
        val årsavregning = hentÅrsavregning(aarsavregningId)
        val årsavregningViaBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID).årsavregning
        if (årsavregning != årsavregningViaBehandlingsresultat) {
            throw RuntimeException("Årsavregning med id: $aarsavregningId hører ikke til Behandling med Id: $behandlingID")
        }

        if (trygdeavgiftFraAvgiftssystemet != null) årsavregning.trygdeavgiftFraAvgiftssystemet = trygdeavgiftFraAvgiftssystemet
        if (beregnetAvgiftBelop != null) årsavregning.beregnetAvgiftBelop = beregnetAvgiftBelop
        if (manueltAvgiftBeloep != null) årsavregning.manueltAvgiftBeloep = manueltAvgiftBeloep
        if (endeligAvgift != null) {
            årsavregning.endeligAvgiftValg = endeligAvgift
            if (endeligAvgift != EndeligAvgiftValg.MANUELL_ENDELIG_AVGIFT) {
                årsavregning.manueltAvgiftBeloep = null
            } else {
                årsavregning.beregnetAvgiftBelop = null
                trygdeavgiftService.slettTrygdeavgiftsperioderPåBehandlingsresultat(behandlingID)
            }
        }

        årsavregning.beregnTilFaktureringsBeloep()

        return lagÅrsavregningModelFraÅrsavregning(årsavregning)
    }

    companion object {
        private const val ANTALL_ÅR_TILBAKE_I_TID = 7  //Fjoråret - 6 år
    }
}

data class ÅrsavregningModel(
    val årsavregningID: Long,
    val år: Int,
    val tidligereGrunnlag: Trygdeavgiftsgrunnlag?,
    val tidligereAvgift: List<Trygdeavgiftsperiode>,
    val nyttGrunnlag: Trygdeavgiftsgrunnlag?,
    val endeligAvgift: List<Trygdeavgiftsperiode>,
    val tidligereFakturertBeloep: BigDecimal?,
    val beregnetAvgiftBelop: BigDecimal?,
    val tilFaktureringBeloep: BigDecimal?,
    val harTrygdeavgiftFraAvgiftssystemet: Boolean?,
    val trygdeavgiftFraAvgiftssystemet: BigDecimal?,
    val endeligAvgiftValg: EndeligAvgiftValg? = null,
    val manueltAvgiftBeloep: BigDecimal?,
    val tidligereTrygdeavgiftFraAvgiftssystemet: BigDecimal?,
    val tidligereÅrsavregningmanueltAvgiftBeloep: BigDecimal?,
    val harSkjoennsfastsattInntektsgrunnlag: Boolean
)

data class Trygdeavgiftsgrunnlag(
    val medlemskapsperioder: List<MedlemskapsperiodeForAvgift>,
    val skatteforholdsperioder: List<SkatteforholdTilNorgeForAvgift>,
    val innteksperioder: List<InntektsperioderForAvgift>
)

private fun avkortFraOgMedDatoForÅr(gjelderÅr: Int, fom: LocalDate): LocalDate = if (fom.year < gjelderÅr) {
    LocalDate.of(gjelderÅr, 1, 1)
} else fom

private fun avkortTilOgMedDatoForÅr(gjelderÅr: Int, tom: LocalDate): LocalDate = if (tom.year > gjelderÅr) {
    LocalDate.of(gjelderÅr, 12, 31)
} else tom

data class MedlemskapsperiodeForAvgift(
    val fom: LocalDate,
    val tom: LocalDate,
    val dekning: Trygdedekninger,
    val bestemmelse: Bestemmelse,
    val medlemskapstyper: Medlemskapstyper
) {
    constructor(medlemskapsperiode: Medlemskapsperiode) : this(
        fom = medlemskapsperiode.fom,
        tom = medlemskapsperiode.tom,
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse,
        medlemskapstyper = medlemskapsperiode.medlemskapstype
    )

    constructor(gjeldendeÅr: Int, medlemskapsperiode: Medlemskapsperiode) : this(
        fom = avkortFraOgMedDatoForÅr(gjeldendeÅr, medlemskapsperiode.fom),
        tom = avkortTilOgMedDatoForÅr(gjeldendeÅr, medlemskapsperiode.tom),
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse,
        medlemskapstyper = medlemskapsperiode.medlemskapstype
    )
}

data class SkatteforholdTilNorgeForAvgift(
    val fom: LocalDate,
    val tom: LocalDate,
    val skatteplikttype: Skatteplikttype,
) {
    constructor(skatteforholdTilNorge: SkatteforholdTilNorge) : this(
        fom = skatteforholdTilNorge.fom,
        tom = skatteforholdTilNorge.tom,
        skatteplikttype = skatteforholdTilNorge.skatteplikttype,
    )

    constructor(gjeldendeÅr: Int, skatteforholdTilNorge: SkatteforholdTilNorge) : this(
        fom = avkortFraOgMedDatoForÅr(gjeldendeÅr, skatteforholdTilNorge.fom),
        tom = avkortTilOgMedDatoForÅr(gjeldendeÅr, skatteforholdTilNorge.tom),
        skatteplikttype = skatteforholdTilNorge.skatteplikttype,
    )
}

data class InntektsperioderForAvgift(
    val fom: LocalDate,
    val tom: LocalDate,
    val type: Inntektskildetype,
    val avgiftspliktigInntekt: Penger?,
    val avgiftspliktigTotalInntekt: Penger?,
    val isArbeidsgiversavgiftBetalesTilSkatt: Boolean,
    val erMaanedsbelop: Boolean
) {
    @Suppress("USELESS_ELVIS_RIGHT_IS_NULL")  // Inntektsperiode er Java-kode, og Kotlin klarer ikke å se at det er nullable
    constructor(inntektsperiode: Inntektsperiode) : this(
        fom = inntektsperiode.fom,
        tom = inntektsperiode.tom,
        type = inntektsperiode.type,
        isArbeidsgiversavgiftBetalesTilSkatt = inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
        avgiftspliktigInntekt = inntektsperiode.avgiftspliktigMndInntekt ?: null,
        avgiftspliktigTotalInntekt = inntektsperiode.avgiftspliktigTotalinntekt ?: null,
        erMaanedsbelop = inntektsperiode.erMaanedsbelop()
    )

    constructor(gjeldendeÅr: Int, inntektsperiode: Inntektsperiode) : this(
        fom = avkortFraOgMedDatoForÅr(gjeldendeÅr, inntektsperiode.fom),
        tom = avkortTilOgMedDatoForÅr(gjeldendeÅr, inntektsperiode.tom),
        type = inntektsperiode.type,
        avgiftspliktigInntekt = inntektsperiode.avgiftspliktigMndInntekt,
        avgiftspliktigTotalInntekt = inntektsperiode.avgiftspliktigTotalinntekt,
        isArbeidsgiversavgiftBetalesTilSkatt = inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
        erMaanedsbelop = inntektsperiode.erMaanedsbelop()
    )
}
