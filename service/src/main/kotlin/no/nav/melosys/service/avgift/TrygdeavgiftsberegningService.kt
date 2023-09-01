package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftsberegningsRequestMapper
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import no.nav.melosys.service.MedlemAvFolketrygdenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.threeten.extra.LocalDateRange
import java.time.DateTimeException
import java.util.*

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

        val innvilgedeMedlemskapsperioder =
            medlemAvFolketrygden.medlemskapsperioder.filter { it.erInnvilget() }

        valider(medlemAvFolketrygden)
        validerInntekstperioderDekkerMedlemskapsperioder(
            fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder,
            innvilgedeMedlemskapsperioder
        )


        fastsattTrygdeavgift.trygdeavgiftsperioder.clear()

        if (!fastsattTrygdeavgift.skalBetalesTilNav()) {
            return emptySet()
        }


        val (trygdeavgiftsberegningRequest, UUID_DBID_MAPS) =
            TrygdeavgiftsberegningsRequestMapper().map(
                innvilgedeMedlemskapsperioder,
                fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge,
                fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder
            )
        val beregnetTrygdeavgift = trygdeavgiftConsumer.beregnTrygdeavgift(trygdeavgiftsberegningRequest)
        oppdaterTrygdeavgift(beregnetTrygdeavgift, fastsattTrygdeavgift, UUID_DBID_MAPS)

        return medlemAvFolketrygdenService.lagre(medlemAvFolketrygden).fastsattTrygdeavgift.trygdeavgiftsperioder
    }

    private fun oppdaterTrygdeavgift(
        beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>,
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        UUID_DBID_MAPS: List<Map<UUID, Long>>
    ) =
        beregnetTrygdeavgift.forEach {
            fastsattTrygdeavgift.trygdeavgiftsperioder.add(
                lagTrygdeavgiftsperiode(
                    fastsattTrygdeavgift,
                    it,
                    UUID_DBID_MAPS
                )
            )
        }

    private fun lagTrygdeavgiftsperiode(
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        trygdeavgiftsberegningResponse: TrygdeavgiftsberegningResponse,
        UUID_DBID_MAPS: List<Map<UUID, Long>>
    ): Trygdeavgiftsperiode {
        val beregnetPeriode = trygdeavgiftsberegningResponse.beregnetPeriode
        val beregningsgrunnlag = trygdeavgiftsberegningResponse.grunnlag

        return Trygdeavgiftsperiode().apply {
            this.periodeFra = beregnetPeriode.periode.fom
            this.periodeTil = beregnetPeriode.periode.tom
            this.trygdesats = beregnetPeriode.sats
            this.trygdeavgiftsbeløpMd = beregnetPeriode.månedsavgift.tilPenger()
            this.fastsattTrygdeavgift = fastsattTrygdeavgift
            this.grunnlagMedlemskapsperiode = fastsattTrygdeavgift.medlemAvFolketrygden.medlemskapsperioder
                .find {
                    it.id == UUID_DBID_MAPS[0][beregningsgrunnlag.medlemskapsperiodeId]
                }
            this.grunnlagSkatteforholdTilNorge = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge
                .find {
                    it.id == UUID_DBID_MAPS[1][beregningsgrunnlag.skatteforholdsperiodeId]
                }
            this.grunnlagInntekstperiode = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder
                .find {
                    it.id == UUID_DBID_MAPS[2][beregningsgrunnlag.inntektsperiodeId]
                }
        }
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
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)?.fastsattTrygdeavgift?.trygdeavgiftsperioder
            ?: emptySet()
    }

    companion object {
        fun validerInntekstperioderDekkerMedlemskapsperioder(
            inntektsperioder: List<Inntektsperiode>,
            medlemskapsperioder: List<Medlemskapsperiode>
        ) {
            val inntektperiodeRange = inntektsperioder.sortedBy { it.fomDato }
                .map { inntektsperiode -> LocalDateRange.ofClosed(inntektsperiode.fomDato, inntektsperiode.tomDato) }

            var totaltRange: LocalDateRange? = null
            try {
                for (range in inntektperiodeRange) {
                    totaltRange = if (totaltRange == null) {
                        range
                    } else {
                        totaltRange.union(range)
                    }
                }
            } catch (ex: DateTimeException) {
                throw FunksjonellException("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
            }


            val sortertMedlemskapsperiode = medlemskapsperioder.sortedBy { it.fom }
            if (LocalDateRange.ofClosed(
                    sortertMedlemskapsperiode.first().fom,
                    sortertMedlemskapsperiode.last().tom
                ) != totaltRange
            ) {
                throw FunksjonellException("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
            }
        }
    }

}
