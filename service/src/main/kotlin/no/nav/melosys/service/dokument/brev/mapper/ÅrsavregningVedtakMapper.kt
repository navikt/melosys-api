package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.kodeverk.EndeligAvgiftValg.MANUELL_ENDELIG_AVGIFT
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Inntektskildetype.MISJONÆR
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.Avgiftsperiode
import no.nav.melosys.integrasjon.dokgen.dto.SvarAlternativ
import no.nav.melosys.integrasjon.dokgen.dto.ÅrsavregningVedtaksbrev
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.aarsavregning.MedlemskapsperiodeForAvgift
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalbeløpBeregner.kalkulertMndInntekt
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningKonstanter
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ÅrsavregningVedtakMapper(
    private val årsavregningService: ÅrsavregningService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService
) {
    @Transactional
    internal fun mapÅrsavregning(
        brevbestilling: ÅrsavregningVedtakBrevBestilling,
        behandlingsresultat: Behandlingsresultat
    ): ÅrsavregningVedtaksbrev {
        val behandlingsId = brevbestilling.behandlingId
        val årsavregningModel = årsavregningService.finnÅrsavregningForBehandling(behandlingsId)
            ?: throw FunksjonellException("Finner ingen årsavregning for behandling $behandlingsId")

        if (årsavregningModel.endeligAvgiftValg == MANUELL_ENDELIG_AVGIFT) {
            return mapManueltBeregnetÅrsavregning(brevbestilling, behandlingsresultat.hentBehandling(), årsavregningModel)
        }

        val fagsak = behandlingsresultat.hentBehandling().fagsak

        val pliktigMedlemskap = harPliktigMedlemskap(årsavregningModel.tidligereGrunnlag?.medlemskapsperioder)
        val pliktigMedlemskapNyttgrunnlag = harPliktigMedlemskap(årsavregningModel.nyttGrunnlag?.medlemskapsperioder)
        val erNyÅrsavregning = behandlingsresultat.årsavregning?.tidligereBehandlingsresultat?.behandling?.erÅrsavregning() ?: false

        return ÅrsavregningVedtaksbrev(
            brevBestilling = brevbestilling,
            årsavregningsår = behandlingsresultat.hentÅrsavregning().aar,
            endeligTrygdeavgift = avgiftsPeriodeMapper(pliktigMedlemskapNyttgrunnlag, årsavregningModel.endeligAvgift),
            forskuddsvisFakturertTrygdeavgift = avgiftsPeriodeMapper(pliktigMedlemskap, årsavregningModel.tidligereAvgift),
            endeligTrygdeavgiftTotalbeløp = årsavregningModel.beregnetAvgiftBelop
                ?: throw FunksjonellException("BeregnetAvgiftBelop finnes ikke for behandling $behandlingsId"),
            forskuddsvisFakturertTrygdeavgiftTotalbeløp = totaltTidligereFakturertBeloep(årsavregningModel),
            differansebeløp = årsavregningModel.tilFaktureringBeloep ?: BigDecimal.ZERO,
            minimumsbeløpForFakturering = ÅrsavregningKonstanter.MINIMUM_BELØP_FAKTURERING.beløp,
            harGrunnlagKunFraMelosys = harGrunnlagKunFraMelosys(årsavregningModel),
            innledningFritekst = brevbestilling.innledningFritekstAarsavregning,
            begrunnelseFritekst = brevbestilling.begrunnelseFritekstAarsavregning,
            pliktigMedlemskap = pliktigMedlemskap,
            eøsEllerTrygdeavtale = fagsak.erSakstypeEøs() || fagsak.erSakstypeTrygdeavtale(),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandlingsresultat.hentBehandling()),
            harSkjoennsfastsattInntektsgrunnlag = årsavregningModel.harSkjoennsfastsattInntektsgrunnlag,
            erNyÅrsavregning = erNyÅrsavregning
        )
    }

    private fun mapManueltBeregnetÅrsavregning(
        brevbestilling: ÅrsavregningVedtakBrevBestilling,
        behandling: Behandling,
        årsavregningModel: ÅrsavregningModel
    ):
        ÅrsavregningVedtaksbrev {
        val fagsak = behandling.fagsak
        val pliktigMedlemskap = harPliktigMedlemskap(årsavregningModel.tidligereGrunnlag?.medlemskapsperioder)
        val erNyÅrsavregning = årsavregningModel.tidligereÅrsavregningmanueltAvgiftBeloep != null

        return ÅrsavregningVedtaksbrev(
            brevBestilling = brevbestilling,
            årsavregningsår = årsavregningModel.år,
            endeligTrygdeavgift = emptyList(),
            forskuddsvisFakturertTrygdeavgift = avgiftsPeriodeMapper(pliktigMedlemskap, årsavregningModel.tidligereAvgift),
            endeligTrygdeavgiftTotalbeløp = årsavregningModel.manueltAvgiftBeloep
                ?: throw FunksjonellException("Manuelt beregnet avgift finnes ikke for behandling ${behandling.id}"),
            forskuddsvisFakturertTrygdeavgiftTotalbeløp = totaltTidligereFakturertBeloep(årsavregningModel),
            differansebeløp = årsavregningModel.tilFaktureringBeloep ?: BigDecimal.ZERO,
            minimumsbeløpForFakturering = ÅrsavregningKonstanter.MINIMUM_BELØP_FAKTURERING.beløp,
            harGrunnlagKunFraMelosys = harGrunnlagKunFraMelosys(årsavregningModel),
            innledningFritekst = brevbestilling.innledningFritekstAarsavregning,
            begrunnelseFritekst = brevbestilling.begrunnelseFritekstAarsavregning,
            pliktigMedlemskap = pliktigMedlemskap,
            eøsEllerTrygdeavtale = fagsak.erSakstypeEøs() || fagsak.erSakstypeTrygdeavtale(),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandling),
            harSkjoennsfastsattInntektsgrunnlag = årsavregningModel.harSkjoennsfastsattInntektsgrunnlag,
            erNyÅrsavregning = erNyÅrsavregning
        )
    }

    private fun avgiftsPeriodeMapper(
        medlemskapsTypePliktig: Boolean,
        trygdeavgiftsperioder: List<Trygdeavgiftsperiode>
    ): List<Avgiftsperiode> {
        if (trygdeavgiftsperioder.all { !it.harAvgift() }) return emptyList()

        return trygdeavgiftsperioder.map { trygdeavgiftsperiode ->
            val grunnlagsInntektsperiode = trygdeavgiftsperiode.grunnlagInntekstperiode
                ?: throw IllegalStateException("trygdeavgiftsperioden må ha en inntektsperiode")

            Avgiftsperiode(
                fom = trygdeavgiftsperiode.fom,
                tom = trygdeavgiftsperiode.tom,
                avgiftssats = trygdeavgiftsperiode.trygdesats,
                avgiftPerMd = trygdeavgiftsperiode.trygdeavgiftsbeløpMd.hentVerdi(),
                avgiftspliktigInntektPerMd = grunnlagsInntektsperiode.kalkulertMndInntekt(),
                inntektskilde = grunnlagsInntektsperiode.type.beskrivelse,
                trygdedekning = trygdeavgiftsperiode.grunnlagMedlemskapsperiodeNotNull.trygdedekning.beskrivelse,
                arbeidsgiveravgiftBetalt = arbeidsGiverAvgiftBetalesTilSkatt(
                    medlemskapsTypePliktig,
                    grunnlagsInntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
                    grunnlagsInntektsperiode.type
                ),
                skatteplikt = trygdeavgiftsperiode.grunnlagSkatteforholdTilNorge!!
                    .skatteplikttype == Skatteplikttype.SKATTEPLIKTIG
            )
        }
    }

    private fun harGrunnlagKunFraMelosys(årsavregning: ÅrsavregningModel): Boolean =
        (årsavregning.harTrygdeavgiftFraAvgiftssystemet == null || årsavregning.harTrygdeavgiftFraAvgiftssystemet != true) && årsavregning.tidligereGrunnlag != null

    private fun totaltTidligereFakturertBeloep(årsavregning: ÅrsavregningModel): BigDecimal {
        return (årsavregning.tidligereFakturertBeloep ?: BigDecimal.ZERO) + (årsavregning.trygdeavgiftFraAvgiftssystemet ?: BigDecimal.ZERO)
    }

    private fun finnFullmektigTrygdeavgift(behandling: Behandling): String? =
        behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
            ?.let { trygdeavgiftsberegningService.finnFakturamottakerNavn(behandling.id) }


    private fun arbeidsGiverAvgiftBetalesTilSkatt(
        medlemskapstypePliktig: Boolean,
        arbeidsgiverAvgiftBetalesTilSkatt: Boolean,
        inntektskildeType: Inntektskildetype
    ): SvarAlternativ = when {
        arbeidsgiverAvgiftBetalesTilSkatt -> SvarAlternativ.JA
        arbAvgBetalesKreves(medlemskapstypePliktig, inntektskildeType) -> SvarAlternativ.NEI
        else -> SvarAlternativ.IKKE_RELEVANT
    }

    private fun arbAvgBetalesKreves(medlemskapsTypeErPliktig: Boolean, inntektskildeType: Inntektskildetype): Boolean {
        return !medlemskapsTypeErPliktig && inntektskildeType !== MISJONÆR
    }

    private fun harPliktigMedlemskap(medlemskapsperioder: List<MedlemskapsperiodeForAvgift>?): Boolean {
        return medlemskapsperioder?.takeIf { it.isNotEmpty() }
            ?.all { it.medlemskapstyper == Medlemskapstyper.PLIKTIG } == true
    }
}
