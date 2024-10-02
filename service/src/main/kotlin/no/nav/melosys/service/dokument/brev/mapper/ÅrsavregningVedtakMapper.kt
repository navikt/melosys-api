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
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class ÅrsavregningVedtakMapper(
    private val årsavregningService: ÅrsavregningService
) {
    @Transactional
    internal fun mapÅrsavregning(brevbestilling: ÅrsavregningVedtakBrevBestilling, behandlingsresultat: Behandlingsresultat): ÅrsavregningVedtaksbrev {

        val årsavregningModel = årsavregningService.finnÅrsavregning(brevbestilling.behandlingId)
            ?: throw FunksjonellException("Finner ingen årsavregning for behandling " + brevbestilling.behandlingId)
        val fagsak = behandlingsresultat.behandling.fagsak

        return ÅrsavregningVedtaksbrev(
            brevBestilling = brevbestilling,
            årsavregningsår = behandlingsresultat.årsavregning.aar,
            endeligTrygdeavgift = avgiftsPeriodeMapper(årsavregningModel.endeligAvgift),
            forskuddsvisFakturertTrygdeavgift = avgiftsPeriodeMapper(årsavregningModel.tidligereAvgift),
            endeligTrygdeavgiftTotalbeløp = årsavregningModel.nyttTotalbeloep,
            forskuddsvisFakturertTrygdeavgiftTotalbeløp = årsavregningModel.tidligereFakturertBeloep,
            differansebeløp = beregnDiffForÅrsavregning(årsavregningModel),
            minimumsbeløpForFakturering = BigDecimal(100),
            pliktigMedlemskap = årsavregningModel.tidligereGrunnlag?.medlemskapsperioder?.all { it.medlemskapstyper == Medlemskapstyper.PLIKTIG },
            eøsEllerTrygdeavtale = fagsak.erSakstypeEøs() || fagsak.erSakstypeTrygdeavtale()
        )
    }

    private fun beregnDiffForÅrsavregning(årsavregningModel: ÅrsavregningModel): BigDecimal {
        val tidligereTrygdeavgift = årsavregningModel.tidligereFakturertBeloep ?: BigDecimal.ZERO
        val nyTrygdeavgift = årsavregningModel.nyttTotalbeloep ?: BigDecimal.ZERO
        return tidligereTrygdeavgift.subtract(nyTrygdeavgift)
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
                    avgiftspliktigInntektPerMd = trygdeavgiftsperiode.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi,
                    inntektskilde = trygdeavgiftsperiode.grunnlagInntekstperiode.type.name,
                    trygdedekning = trygdeavgiftsperiode.grunnlagMedlemskapsperiode.trygdedekning.name,
                    arbeidsgiveravgiftBetalt = trygdeavgiftsperiode.grunnlagInntekstperiode.isArbeidsgiversavgiftBetalesTilSkatt,
                    skatteplikt = trygdeavgiftsperiode.grunnlagSkatteforholdTilNorge.skatteplikttype.equals(Skatteplikttype.SKATTEPLIKTIG)
                )
            )
        }
        return avgiftsperioder
    }
}
