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

        return opprettEllerOppdaterÅrsavregning(behandlingsresultat, eksisterendeÅrsavregning.aar)
    }

    @Transactional
    fun opprettÅrsavregning(behandlingID: Long, gjelderÅr: Int): ÅrsavregningModel {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        if (behandlingsresultat.årsavregning != null && behandlingsresultat.årsavregning?.aar == gjelderÅr) {
            return lagÅrsavregningModelFraÅrsavregning(behandlingsresultat.hentÅrsavregning())
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

        // Henter siste relevante behandlinger - kan være forskjellige for medlemskapsperiode og avgiftsgrunnlag
        val sisteRelevanteBehandlinger = hentGjeldendeBehandlingsresultaterForÅrsavregning(
            behandlingsresultat.hentBehandling().fagsak.saksnummer,
            gjelderÅr
        )

        val sisteBehandlingsresultatMedMedlemskapsperiode = sisteRelevanteBehandlinger?.sisteBehandlingsresultatMedMedlemskapsperiode

        // Replikerer medlemskapsperioder fra siste behandling med medlemskap
        if (sisteBehandlingsresultatMedMedlemskapsperiode != null) {
            replikerMedlemskapsperioder(
                behandlingsresultat,
                sisteBehandlingsresultatMedMedlemskapsperiode,
                gjelderÅr
            )
        }

        val sisteÅrsavregning = sisteRelevanteBehandlinger?.sisteÅrsavregning?.årsavregning

        val årsavregning = Årsavregning(
            aar = gjelderÅr,
            behandlingsresultat = behandlingsresultat,
            tidligereBehandlingsresultat = sisteBehandlingsresultatMedMedlemskapsperiode,
            tidligereFakturertBeloep =
                sisteÅrsavregning?.manueltAvgiftBeloep
                    ?: TotalbeløpBeregner.hentTotalavgift(
                        sisteRelevanteBehandlinger?.sisteBehandlingsresultatMedAvgift?.trygdeavgiftsperioder?.filter {
                            it.overlapperMedÅr(
                                gjelderÅr
                            )
                        }.orEmpty()
                    ),
            endeligAvgiftValg = sisteÅrsavregning?.endeligAvgiftValg ?: EndeligAvgiftValg.OPPLYSNINGER_ENDRET,
            harTrygdeavgiftFraAvgiftssystemet = sisteÅrsavregning?.let { it.harTrygdeavgiftFraAvgiftssystemet ?: true },
            trygdeavgiftFraAvgiftssystemet = sisteÅrsavregning?.trygdeavgiftFraAvgiftssystemet,
            manueltAvgiftBeloep = sisteÅrsavregning?.manueltAvgiftBeloep
        ).let { årsavregning ->
            behandlingsresultat.årsavregning = årsavregning
            behandlingsresultatService.lagre(årsavregning.hentBehandlingsresultat).hentÅrsavregning()
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
            .filter { førVedtaksdato == null || it.hentVedtakMetadata().vedtaksdato < førVedtaksdato }
            .sortedBy { it.hentVedtakMetadata().vedtaksdato }
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

            if (årsavregning.tidligereBehandlingsresultat !== null && årsavregning.hentTidligereBehandlingsresultat.medlemskapsperioder !== null) {
                replikerMedlemskapsperioder(
                    behandlingsresultat,
                    årsavregning.hentTidligereBehandlingsresultat,
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

        val vedtaksDato = årsavregning.behandlingsresultat?.vedtakMetadata?.vedtaksdato

        val sisteÅrsavregning = hentSisteÅrsavregning(årsavregning.hentBehandlingsresultat.hentBehandling().fagsak.saksnummer, år, vedtaksDato)

        return ÅrsavregningModel(
            årsavregningID = årsavregning.id,
            år = år,
            tidligereTrygdeavgiftsGrunnlag = hentTidligereTrygdeavgiftsgrunnlag(år, årsavregning.behandlingsresultat?.behandling?.fagsak?.saksnummer),
            sisteGjeldendeMedlemskapsperioder = hentSisteGjeldendeMedlemskapsperioder(år, årsavregning.behandlingsresultat?.behandling?.fagsak?.saksnummer),
            tidligereAvgift = hentTidligereAvgift(år, årsavregning.behandlingsresultat?.behandling?.fagsak?.saksnummer),
            nyttTrygdeavgiftsGrunnlag = hentNyttTrygdeavgiftsgrunnlag(årsavregning),
            endeligAvgift = årsavregning.hentBehandlingsresultat.trygdeavgiftsperioder.toList(),
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

    /**
     * Henter siste relevante behandlingsresultater for årsavregning.
     * Returnerer separate behandlinger for medlemskapsperiode og avgiftsgrunnlag,
     * siden disse kan komme fra forskjellige behandlinger i noen tilfeller.
     */
    fun hentGjeldendeBehandlingsresultaterForÅrsavregning(
        saksnummer: String,
        år: Int,
    ): GjeldendeBehandlingsresultaterForÅrsavregning? {
        val fagsak = fagsakService.hentFagsak(saksnummer)

        if (fagsak.status in UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT) {
            return null
        }

        val behandlingsresultattyper = listOf(
            Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT,
            Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
            Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        )

        val behandlingsresultater = fagsak.behandlinger
            .filter { it.erAvsluttet() }
            .map { behandlingsresultatService.hentBehandlingsresultat(it.id) }
            .filter { it.type in behandlingsresultattyper }
            .filter { it.harInnvilgetMedlemskapsperiodeSomOverlapperMedÅr(år) || harManueltSattAvgift(it, år) }
            .sortedBy { it.registrertDato }


        if (behandlingsresultater.isEmpty()) {
            return null
        }

        // Finner siste behandling med medlemskapsperioder (brukes for gjeldende medlemskap)
        val sisteBehandlingsresultatMedMedlemskapsperiode = behandlingsresultater.lastOrNull { it.medlemskapsperioder.isNotEmpty() }

        // Finner siste behandling med trygdeavgiftsperioder (brukes for avgiftsgrunnlag)
        val sisteBehandlingsresultatMedAvgiftsgrunnlag = behandlingsresultater
            .filter { it.harTrygdeavgiftsperioderSomOverlapperMedÅr(år) }
            .sortedBy { it.registrertDato }

        val sisteÅrsavregning = behandlingsresultater.filter { it.årsavregning != null && it.årsavregning.aar == år }.maxByOrNull { it.registrertDato }

        return GjeldendeBehandlingsresultaterForÅrsavregning(
            sisteBehandlingsresultatMedMedlemskapsperiode = sisteBehandlingsresultatMedMedlemskapsperiode,
            sisteBehandlingsresultatMedAvgift = sisteBehandlingsresultatMedAvgiftsgrunnlag.lastOrNull(),
            sisteÅrsavregning = sisteÅrsavregning
        )
    }

    private fun harManueltSattAvgift(it: Behandlingsresultat, år: Int) =
        it.årsavregning != null && it.hentÅrsavregning().manueltAvgiftBeloep != null && it.hentÅrsavregning().aar == år

    private fun hentTidligereTrygdeavgiftsgrunnlag(år: Int, saksnummer: String?): Trygdeavgiftsgrunnlag? {
        if (saksnummer == null) return null

        val sisteRelevanteBehandlinger = hentGjeldendeBehandlingsresultaterForÅrsavregning(saksnummer, år)

        val sisteBehandlingsresultatMedAvgift = sisteRelevanteBehandlinger?.sisteBehandlingsresultatMedAvgift
        if (sisteBehandlingsresultatMedAvgift == null || sisteBehandlingsresultatMedAvgift.trygdeavgiftsperioder.isEmpty()) {
            return Trygdeavgiftsgrunnlag(
                medlemskapsperioder = emptyList(),
                skatteforholdsperioder = emptyList(),
                innteksperioder = emptyList()
            )
        }

        return Trygdeavgiftsgrunnlag(
            medlemskapsperioder = sisteBehandlingsresultatMedAvgift.medlemskapsperioder.filter { it.overlapperMedÅr(år) && it.erInnvilget() }
                .map { MedlemskapsperiodeForAvgift(år, it) },
            skatteforholdsperioder = sisteBehandlingsresultatMedAvgift.hentSkatteforholdTilNorge().filter { it.overlapperMedÅr(år) }
                .map { SkatteforholdTilNorgeForAvgift(år, it) },
            innteksperioder = sisteBehandlingsresultatMedAvgift.hentInntektsperioder().filter { it.overlapperMedÅr(år) }
                .map { InntektsperioderForAvgift(år, it) }
        )
    }

    private fun hentSisteGjeldendeMedlemskapsperioder(år: Int, saksnummer: String?): List<MedlemskapsperiodeForAvgift> {
        if (saksnummer == null) return emptyList()

        val gjeldendeBehandlingsresultater = hentGjeldendeBehandlingsresultaterForÅrsavregning(saksnummer, år)
        if (gjeldendeBehandlingsresultater == null || gjeldendeBehandlingsresultater.sisteBehandlingsresultatMedMedlemskapsperiode == null) {
            return emptyList()
        }

        return gjeldendeBehandlingsresultater.sisteBehandlingsresultatMedMedlemskapsperiode.medlemskapsperioder
            .filter { it.overlapperMedÅr(år) && it.erInnvilget() }
            .map { MedlemskapsperiodeForAvgift(år, it) }
    }

    private fun hentTidligereAvgift(år: Int, saksnummer: String?): List<Trygdeavgiftsperiode> {
        if (saksnummer == null) return emptyList()

        val sisteRelevanteBehandlinger = hentGjeldendeBehandlingsresultaterForÅrsavregning(saksnummer, år)

        val behandlingsresultat = sisteRelevanteBehandlinger?.sisteBehandlingsresultatMedAvgift
            ?: return emptyList()

        return behandlingsresultat.trygdeavgiftsperioder.filter { it.overlapperMedÅr(år) }
    }

    private fun hentNyttTrygdeavgiftsgrunnlag(årsavregning: Årsavregning): Trygdeavgiftsgrunnlag? {
        val behandlingsresultat = årsavregning.hentBehandlingsresultat
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
    val tidligereTrygdeavgiftsGrunnlag: Trygdeavgiftsgrunnlag? = null,
    val sisteGjeldendeMedlemskapsperioder: List<MedlemskapsperiodeForAvgift> = emptyList(),
    val tidligereAvgift: List<Trygdeavgiftsperiode>,
    val nyttTrygdeavgiftsGrunnlag: Trygdeavgiftsgrunnlag? = null,
    val endeligAvgift: List<Trygdeavgiftsperiode>,
    val tidligereFakturertBeloep: BigDecimal? = null,
    val beregnetAvgiftBelop: BigDecimal? = null,
    val tilFaktureringBeloep: BigDecimal? = null,
    val harTrygdeavgiftFraAvgiftssystemet: Boolean? = null,
    val trygdeavgiftFraAvgiftssystemet: BigDecimal? = null,
    val endeligAvgiftValg: EndeligAvgiftValg? = null,
    val manueltAvgiftBeloep: BigDecimal? = null,
    val tidligereTrygdeavgiftFraAvgiftssystemet: BigDecimal? = null,
    val tidligereÅrsavregningmanueltAvgiftBeloep: BigDecimal? = null,
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
    val medlemskapstyper: Medlemskapstyper,
    val innvilgelsesresultat: InnvilgelsesResultat,
) {
    constructor(medlemskapsperiode: Medlemskapsperiode) : this(
        fom = medlemskapsperiode.fom,
        tom = medlemskapsperiode.tom,
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse,
        medlemskapstyper = medlemskapsperiode.medlemskapstype,
        innvilgelsesresultat = medlemskapsperiode.innvilgelsesresultat,
    )

    constructor(gjeldendeÅr: Int, medlemskapsperiode: Medlemskapsperiode) : this(
        fom = avkortFraOgMedDatoForÅr(gjeldendeÅr, medlemskapsperiode.fom),
        tom = avkortTilOgMedDatoForÅr(gjeldendeÅr, medlemskapsperiode.tom),
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse,
        medlemskapstyper = medlemskapsperiode.medlemskapstype,
        innvilgelsesresultat = medlemskapsperiode.innvilgelsesresultat
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

data class GjeldendeBehandlingsresultaterForÅrsavregning(
    val sisteBehandlingsresultatMedMedlemskapsperiode: Behandlingsresultat? = null,
    val sisteBehandlingsresultatMedAvgift: Behandlingsresultat? = null,
    val sisteÅrsavregning: Behandlingsresultat? = null
)
