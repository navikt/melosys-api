package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Inntektskildetype.MISJONÆR
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.Avgiftsperiode
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

        val fagsak = behandlingsresultat.behandling.fagsak

        val pliktigMedlemskap = pliktigMedlemskap(årsavregningModel.tidligereGrunnlag?.medlemskapsperioder)
        val pliktigMedlemskapNyttgrunnlag = pliktigMedlemskap(årsavregningModel.nyttGrunnlag?.medlemskapsperioder)


        return ÅrsavregningVedtaksbrev(
            brevBestilling = brevbestilling,
            årsavregningsår = behandlingsresultat.årsavregning.aar,
            endeligTrygdeavgift = avgiftsPeriodeMapper(pliktigMedlemskapNyttgrunnlag, årsavregningModel.endeligAvgift),
            forskuddsvisFakturertTrygdeavgift = avgiftsPeriodeMapper(pliktigMedlemskap, årsavregningModel.tidligereAvgift),
            endeligTrygdeavgiftTotalbeløp = årsavregningModel.nyttTotalbeloep
                ?: throw FunksjonellException("Nytt totalbeløp finnes ikke for behandling $behandlingsId"),
            forskuddsvisFakturertTrygdeavgiftTotalbeløp = totaltTidligereFakturertBeloep(årsavregningModel),
            differansebeløp = regnUtDifferanseBeløp(årsavregningModel),
            minimumsbeløpForFakturering = ÅrsavregningKonstanter.MINIMUM_BELØP_FAKTURERING.beløp,
            harGrunnlagKunFraMelosys = harGrunnlagKunFraMelosys(årsavregningModel),
            pliktigMedlemskap = pliktigMedlemskap,
            eøsEllerTrygdeavtale = fagsak.erSakstypeEøs() || fagsak.erSakstypeTrygdeavtale(),
            fullmektigTrygdeavgift = finnFullmektigTrygdeavgift(behandlingsresultat.behandling),
        )
    }

    private fun avgiftsPeriodeMapper(medlemskapsTypePliktig: Boolean, trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): List<Avgiftsperiode> {
        val avgiftsperioder = ArrayList<Avgiftsperiode>()


        val harKunSkattepliktigTrygdeavgiftsperioder = trygdeavgiftsperioder.all { it.grunnlagInntekstperiode == null }
        if (harKunSkattepliktigTrygdeavgiftsperioder) {
            return avgiftsperioder
        }

        for (trygdeavgiftsperiode in trygdeavgiftsperioder) {
            val grunnlagsInntektsperiode = trygdeavgiftsperiode.grunnlagInntekstperiode
                ?: throw IllegalStateException("grunnlagInntekstperiode cannot be null")

            val arbeidsGiverAvgiftBetalesTilSkatt = arbeidsGiverAvgiftBetalesTilSkatt(
                medlemskapsTypePliktig,
                grunnlagsInntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt,
                grunnlagsInntektsperiode.type
            )

            avgiftsperioder.add(
                Avgiftsperiode(
                    fom = trygdeavgiftsperiode.fom,
                    tom = trygdeavgiftsperiode.tom,
                    avgiftssats = trygdeavgiftsperiode.trygdesats,
                    avgiftPerMd = trygdeavgiftsperiode.trygdeavgiftsbeløpMd.verdi,
                    avgiftspliktigInntektPerMd = trygdeavgiftsperiode.grunnlagInntekstperiode!!.kalkulertMndInntekt(),
                    inntektskilde = trygdeavgiftsperiode.grunnlagInntekstperiode!!.type.beskrivelse,
                    trygdedekning = trygdeavgiftsperiode.grunnlagMedlemskapsperiodeNotNull.trygdedekning.beskrivelse,
                    arbeidsgiveravgiftBetalt = arbeidsGiverAvgiftBetalesTilSkatt,
                    skatteplikt = trygdeavgiftsperiode.grunnlagSkatteforholdTilNorge!!.skatteplikttype.equals(Skatteplikttype.SKATTEPLIKTIG)
                )
            )
        }
        return avgiftsperioder
    }

    private fun harGrunnlagKunFraMelosys(årsavregning: ÅrsavregningModel): Boolean =
        (årsavregning.harDeltGrunnlag == null || årsavregning.harDeltGrunnlag != true) && årsavregning.tidligereGrunnlag != null

    private fun regnUtDifferanseBeløp(årsavregning: ÅrsavregningModel): BigDecimal {
        return årsavregning.nyttTotalbeloep?.subtract(totaltTidligereFakturertBeloep(årsavregning))
            ?: throw FunksjonellException("Nytt totalbeløp finnes ikke")
    }

    private fun totaltTidligereFakturertBeloep(årsavregning: ÅrsavregningModel): BigDecimal {
        return (årsavregning.tidligereFakturertBeloep ?: BigDecimal.ZERO) + (årsavregning.tidligereFakturertBeloepAvgiftssystem ?: BigDecimal.ZERO)
    }

    private fun finnFullmektigTrygdeavgift(behandling: Behandling): String? {
        if (behandling.fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT) == null) return null

        return trygdeavgiftsberegningService.finnFakturamottakerNavn(behandling.id)
    }

    private fun arbeidsGiverAvgiftBetalesTilSkatt(
        medlemskapstypePliktig: Boolean,
        arbeidsgiverAvgiftBetalesTilSkatt: Boolean,
        inntektskildeType: Inntektskildetype
    ): Boolean? {
        if (!arbeidsgiverAvgiftBetalesTilSkatt) {
            val arbeidsavgiverAvgiftKreves = arbAvgBetalesKreves(medlemskapstypePliktig, inntektskildeType)
            return if (!arbeidsavgiverAvgiftKreves) {
                null
            } else {
                arbeidsgiverAvgiftBetalesTilSkatt
            }

        }

        return arbeidsgiverAvgiftBetalesTilSkatt
    }

    private fun arbAvgBetalesKreves(medlemskapsTypeErPliktig: Boolean, inntektskildeType: Inntektskildetype): Boolean {
        return !medlemskapsTypeErPliktig && inntektskildeType !== MISJONÆR
    }

    private fun pliktigMedlemskap(medlemskapsperioder: List<MedlemskapsperiodeForAvgift>?): Boolean {
        return medlemskapsperioder?.takeIf { it.isNotEmpty() }
            ?.all { it.medlemskapstyper == Medlemskapstyper.PLIKTIG } == true
    }
}
