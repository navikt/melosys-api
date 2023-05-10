package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftBeregningsgrunnlagDtoMapper
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsperiodeDto
import no.nav.melosys.service.MedlemAvFolketrygdenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TrygdeavgiftsberegningService
    (
    private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer,
    private val trygdeavgiftBeregningsgrunnlagDtoMapper: TrygdeavgiftBeregningsgrunnlagDtoMapper
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

        val (trygdeavgiftBeregningsgrunnlagDto, UUID_DBID_MAPS) =
            trygdeavgiftBeregningsgrunnlagDtoMapper.map(
                medlemAvFolketrygden.medlemskapsperioder,
                fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge,
                fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder
            )
        val beregnetTrygdeavgift = trygdeavgiftConsumer.beregnTrygdeavgift(trygdeavgiftBeregningsgrunnlagDto)
        oppdaterTrygdeavgift(beregnetTrygdeavgift, fastsattTrygdeavgift, UUID_DBID_MAPS)

        return medlemAvFolketrygdenService.lagre(medlemAvFolketrygden).fastsattTrygdeavgift.trygdeavgift
    }

    private fun oppdaterTrygdeavgift(
        beregnetTrygdeavgift: List<TrygdeavgiftsperiodeDto>,
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        UUID_DBID_MAPS: List<Map<UUID, Long>>
    ) =
        beregnetTrygdeavgift.forEach {
            fastsattTrygdeavgift.trygdeavgift.add(lagTrygdeavgiftsperiode(fastsattTrygdeavgift, it, UUID_DBID_MAPS))
        }

    private fun lagTrygdeavgiftsperiode(
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        trygdeavgiftsperiodeDto: TrygdeavgiftsperiodeDto,
        UUID_DBID_MAPS: List<Map<UUID, Long>>
    ) =
        Trygdeavgiftsperiode().apply {
            this.periodeFra = trygdeavgiftsperiodeDto.periode.fom
            this.periodeTil = trygdeavgiftsperiodeDto.periode.tom
            this.trygdesats = trygdeavgiftsperiodeDto.sats
            this.trygdeavgiftsbeløpMd = trygdeavgiftsperiodeDto.avgift.tilPenger()
            this.fastsattTrygdeavgift = fastsattTrygdeavgift
            this.grunnlagInntekstperiode = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder
                .find {
                    it.id == UUID_DBID_MAPS.get(0).get(trygdeavgiftsperiodeDto.grunnlagInntektsperiode)
                }
            this.grunnlagMedlemskapsperiode = fastsattTrygdeavgift.medlemAvFolketrygden.medlemskapsperioder
                .find {
                    it.id == UUID_DBID_MAPS.get(1).get(trygdeavgiftsperiodeDto.grunnlagMedlemskapsperiode)
                }
            this.grunnlagSkatteforholdTilNorge = fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge
                .find {
                    it.id == UUID_DBID_MAPS.get(2).get(trygdeavgiftsperiodeDto.grunnlagSkatteforholdsperiode)
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
        return medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)?.fastsattTrygdeavgift?.trygdeavgift
            ?: emptySet()
    }
}
