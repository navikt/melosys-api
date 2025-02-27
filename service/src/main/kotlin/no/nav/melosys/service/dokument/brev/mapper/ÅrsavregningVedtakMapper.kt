package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.Avgiftsperiode
import no.nav.melosys.integrasjon.dokgen.dto.ÅrsavregningVedtaksbrev
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalbeløpBeregner.kalkulertMndInntekt
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningKonstanter
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ÅrsavregningVedtakMapper(
    private val årsavregningService: ÅrsavregningService
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

        return ÅrsavregningVedtaksbrev(
            brevBestilling = brevbestilling,
            årsavregningsår = behandlingsresultat.årsavregning.aar,
            endeligTrygdeavgift = avgiftsPeriodeMapper(årsavregningModel.endeligAvgift),
            forskuddsvisFakturertTrygdeavgift = avgiftsPeriodeMapper(årsavregningModel.tidligereAvgift),
            endeligTrygdeavgiftTotalbeløp = årsavregningModel.nyttTotalbeloep
                ?: throw FunksjonellException("Nytt totalbeløp finnes ikke for behandling $behandlingsId"),
            forskuddsvisFakturertTrygdeavgiftTotalbeløp = årsavregningModel.tidligereFakturertBeloep ?: BigDecimal.ZERO,
            differansebeløp = regnUtDifferanseBeløp(årsavregningModel),
            minimumsbeløpForFakturering = ÅrsavregningKonstanter.MINIMUM_BELØP_FAKTURERING.beløp,
            pliktigMedlemskap = årsavregningModel.tidligereGrunnlag?.medlemskapsperioder?.all { it.medlemskapstyper == Medlemskapstyper.PLIKTIG }
                ?: false,
            eøsEllerTrygdeavtale = fagsak.erSakstypeEøs() || fagsak.erSakstypeTrygdeavtale(),
        )
    }

    private fun avgiftsPeriodeMapper(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): List<Avgiftsperiode> {
        val avgiftsperioder = ArrayList<Avgiftsperiode>()

        for (trygdeavgiftsperiode in trygdeavgiftsperioder) {
            avgiftsperioder.add(
                Avgiftsperiode(
                    fom = trygdeavgiftsperiode.fom,
                    tom = trygdeavgiftsperiode.tom,
                    avgiftssats = trygdeavgiftsperiode.trygdesats,
                    avgiftPerMd = trygdeavgiftsperiode.trygdeavgiftsbeløpMd.verdi,
                    avgiftspliktigInntektPerMd = trygdeavgiftsperiode.grunnlagInntekstperiode!!.kalkulertMndInntekt(),
                    inntektskilde = trygdeavgiftsperiode.grunnlagInntekstperiode!!.type.beskrivelse,
                    trygdedekning = trygdeavgiftsperiode.grunnlagMedlemskapsperiodeNotNull.trygdedekning.beskrivelse,
                    arbeidsgiveravgiftBetalt = trygdeavgiftsperiode.grunnlagInntekstperiode!!.isArbeidsgiversavgiftBetalesTilSkatt,
                    skatteplikt = trygdeavgiftsperiode.grunnlagSkatteforholdTilNorge!!.skatteplikttype.equals(Skatteplikttype.SKATTEPLIKTIG)
                )
            )
        }
        return avgiftsperioder
    }

    private fun regnUtDifferanseBeløp(årsavregningModel: ÅrsavregningModel): BigDecimal {
        val tidligereFakturert = årsavregningModel.tidligereFakturertBeloep ?: BigDecimal.ZERO
        return årsavregningModel.nyttTotalbeloep?.subtract(tidligereFakturert)
            ?: throw FunksjonellException("Nytt totalbeløp finnes ikke")
    }
}
