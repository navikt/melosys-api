package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgift
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.MelosysTrygdeavgfitBeregningV2Dto
import no.nav.melosys.integrasjon.trygdeavgift.dto.Trygdeavgiftsperiode
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
    fun beregnTrygdeavgift(behandlingsresultatID: Long): Set<Trygdeavgift> {
        val medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)
        val fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift

        valider(medlemAvFolketrygden)

        fastsattTrygdeavgift.trygdeavgift.clear()

        if (fastsattTrygdeavgift.getTrygdeavgiftMottaker() == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT) {
            return emptySet()
        }

        beregnOgOppdaterTrygdeavgift(
            medlemAvFolketrygden.medlemskapsperioder,
            fastsattTrygdeavgift.trygdeavgiftsgrunnlag,
            fastsattTrygdeavgift
        )
        return medlemAvFolketrygdenService.lagre(medlemAvFolketrygden).fastsattTrygdeavgift.trygdeavgift
    }

    private fun beregnOgOppdaterTrygdeavgift(
        medlemskapsperioder: Collection<Medlemskapsperiode>,
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag,
        fastsattTrygdeavgift: FastsattTrygdeavgift
    ) {
        val trygdeavgiftsperioder = trygdeavgiftConsumer.beregnTrygdeavgift(
            MelosysTrygdeavgfitBeregningV2Dto.av(
                medlemskapsperioder,
                trygdeavgiftsgrunnlag.skatteforholdTilNorge,
                trygdeavgiftsgrunnlag.inntektsperioder
            )
        )

        trygdeavgiftsperioder.forEach {
            fastsattTrygdeavgift.trygdeavgift.add(lagTrygdeavgift(fastsattTrygdeavgift, it))
        }
    }

    private fun lagTrygdeavgift(
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        trygdeavgiftsperiode: Trygdeavgiftsperiode
    ) =
        Trygdeavgift().apply {
            this.periodeFra = trygdeavgiftsperiode.periode.fom
            this.periodeTil = trygdeavgiftsperiode.periode.tom
            this.trygdesats = trygdeavgiftsperiode.sats
            this.trygdeavgiftsbeløpMd = trygdeavgiftsperiode.avgift.verdi
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
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgift> {
        return medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)?.fastsattTrygdeavgift?.trygdeavgift
            ?: emptySet()
    }
}
