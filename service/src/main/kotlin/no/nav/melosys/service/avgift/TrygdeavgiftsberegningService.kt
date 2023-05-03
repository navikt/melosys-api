package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftBeregningsgrunnlagDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsperiodeDto
import no.nav.melosys.service.MedlemAvFolketrygdenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrygdeavgiftsberegningService
    (
    private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer
) {

    @Transactional
    fun beregnOgLagreTrygdeavgift(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        val medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)
        val fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift

        valider(medlemAvFolketrygden)

        fastsattTrygdeavgift.trygdeavgift.clear()

        if (!fastsattTrygdeavgift.skalBetaleTrygdeavgiftTilNav()) {
            return emptySet()
        }

        val beregnetTrygdeavgift =
            beregnTrygdeavgift(medlemAvFolketrygden.medlemskapsperioder, fastsattTrygdeavgift.trygdeavgiftsgrunnlag)
        oppdaterTrygdeavgift(beregnetTrygdeavgift, fastsattTrygdeavgift)
        return medlemAvFolketrygdenService.lagre(medlemAvFolketrygden).fastsattTrygdeavgift.trygdeavgift
    }

    private fun beregnTrygdeavgift(
        medlemskapsperioder: Collection<Medlemskapsperiode>,
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag,
    ): List<TrygdeavgiftsperiodeDto> =
        trygdeavgiftConsumer.beregnTrygdeavgift(
            TrygdeavgiftBeregningsgrunnlagDto.av(
                medlemskapsperioder,
                trygdeavgiftsgrunnlag.skatteforholdTilNorge,
                trygdeavgiftsgrunnlag.inntektsperioder
            )
        )

    private fun oppdaterTrygdeavgift(
        beregnetTrygdeavgift: List<TrygdeavgiftsperiodeDto>,
        fastsattTrygdeavgift: FastsattTrygdeavgift
    ) =
        beregnetTrygdeavgift.forEach {
            fastsattTrygdeavgift.trygdeavgift.add(lagTrygdeavgiftsperiode(fastsattTrygdeavgift, it))
        }

    private fun lagTrygdeavgiftsperiode(
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        trygdeavgiftsperiodeDto: TrygdeavgiftsperiodeDto
    ) =
        Trygdeavgiftsperiode().apply {
            this.periodeFra = trygdeavgiftsperiodeDto.periode.fom
            this.periodeTil = trygdeavgiftsperiodeDto.periode.tom
            this.trygdesats = trygdeavgiftsperiodeDto.sats
            this.trygdeavgiftsbeløpMd = trygdeavgiftsperiodeDto.avgift.tilPenger()
            this.fastsattTrygdeavgift = fastsattTrygdeavgift
        }

    private fun valider(medlemAvFolketrygden: MedlemAvFolketrygden) {
        if (medlemAvFolketrygden.medlemskapsperioder.isEmpty()) {
            throw FunksjonellException("Kan ikke beregne trygdeavgift uten medlemskapsperioder")
        }
        if (medlemAvFolketrygden.fastsattTrygdeavgift == null) {
            throw FunksjonellException("Kan ikke beregne trygdeavgift uten fastsattTrygdeavgift")
        }
        if (medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag == null) {
            throw FunksjonellException("Kan ikke beregne trygdeavgift uten trygdeavgiftsgrunnlag")
        }
        if (medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.isEmpty()) {
            throw FunksjonellException("Kan ikke beregne trygdeavgift uten skatteforholdTilNorge")
        }
        if (medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.isEmpty()) {
            throw FunksjonellException("Kan ikke beregne trygdeavgift uten inntektsperioder")
        }
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<no.nav.melosys.domain.avgift.Trygdeavgiftsperiode> {
        return medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)?.fastsattTrygdeavgift?.trygdeavgift
            ?: emptySet()
    }
}
