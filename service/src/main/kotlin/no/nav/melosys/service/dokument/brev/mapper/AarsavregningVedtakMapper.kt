package no.nav.melosys.service.dokument.brev.mapper

import jakarta.transaction.Transactional
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.dokgen.dto.ÅrsavregningVedtaksbrev
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class AarsavregningVedtakMapper(
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val dokgenMapperDatahenter: DokgenMapperDatahenter,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val årsavregningService: ÅrsavregningService
) {
    @Transactional
    internal fun mapÅrsavregning(brevbestilling: ÅrsavregningVedtakBrevBestilling, behandlingsresultat: Behandlingsresultat): ÅrsavregningVedtaksbrev {

        val årsavregningModel = årsavregningService.finnÅrsavregning(brevbestilling.behandlingId)
            ?: throw FunksjonellException("Finner ingen årsavregning for behandling " + brevbestilling.behandlingId)

        return ÅrsavregningVedtaksbrev(
            brevBestilling = brevbestilling,
            årsavregningsår = behandlingsresultat.årsavregning.aar,
            endeligTrygdeavgift = årsavregningModel.endeligAvgift,
            forskuddsvisFakturertTrygdeavgift = årsavregningModel.tidligereAvgift,
            endeligTrygdeavgiftTotalbeløp = årsavregningModel.nyttTotalbeloep,
            forskuddsvisFakturertTrygdeavgiftTotalbeløp = årsavregningModel.tidligereFakturertBeloep,
            differansebeløp = beregnDiffForÅrsavregning(årsavregningModel),
            minimumsbeløpForFakturering = BigDecimal(100),
            pliktigMedlemskap = årsavregningModel.tidligereGrunnlag?.medlemskapsperioder?.all { it.medlemskapstyper == Medlemskapstyper.PLIKTIG },
            eøsEllerTrygdeavtale = årsavregningModel.tidligereGrunnlag.medlemskapsperioder //TODO sjekk hvilke typer vi skal bruke.
        )
    }

    private fun beregnDiffForÅrsavregning(årsavregningModel: ÅrsavregningModel): BigDecimal {
        val tidligereTrygdeavgift = årsavregningModel.tidligereFakturertBeloep ?: BigDecimal.ZERO
        val nyTrygdeavgift = årsavregningModel.nyttTotalbeloep ?: BigDecimal.ZERO
        return tidligereTrygdeavgift.subtract(nyTrygdeavgift)
    }
}
