package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftsberegningsRequestMapper
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class TrygdeavgiftsberegningService
    (
    private val behandlingService: BehandlingService,
    private val eregFasade: EregFasade,
    private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val persondataService: PersondataService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer
) {

    @Transactional
    fun beregnOgLagreTrygdeavgift(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        val medlemAvFolketrygden = medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingsresultatID)
        val fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift
        valider(medlemAvFolketrygden)
        fastsattTrygdeavgift.trygdeavgiftsperioder.clear()
        if (!trygdeavgiftMottakerService.skalBetalesTilNav(fastsattTrygdeavgift.trygdeavgiftsgrunnlag)) { return emptySet() }

        val innvilgedeMedlemskapsperioder =
            medlemAvFolketrygden.medlemskapsperioder.filter { it.erInnvilget()}

        val (trygdeavgiftsberegningRequest, UUID_DBID_MAPS) =
            TrygdeavgiftsberegningsRequestMapper().map(
                innvilgedeMedlemskapsperioder,
                fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge,
                fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder,
                hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID, innvilgedeMedlemskapsperioder)
            )
        val beregnetTrygdeavgift = trygdeavgiftConsumer.beregnTrygdeavgift(trygdeavgiftsberegningRequest)
        oppdaterTrygdeavgift(beregnetTrygdeavgift, fastsattTrygdeavgift, UUID_DBID_MAPS)

        return medlemAvFolketrygdenService.lagre(medlemAvFolketrygden).fastsattTrygdeavgift.trygdeavgiftsperioder
    }

    private fun hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID: Long, medlemskapsperioder: List<Medlemskapsperiode>): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig()}) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBruker().aktørId).fødselsdato
        }
        return null
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

        val inntektskilderRequest = medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.map { InntektskildeRequest(it) }
        val skatteforholdTilNorgeRequest = medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.map { SkatteforholdTilNorgeRequest(it) }
        val innvilgedeMedlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder.filter { it.erInnvilget() }.toList()

        TrygdeavgiftsgrunnlagService.validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(inntektskilderRequest, innvilgedeMedlemskapsperioder)
        TrygdeavgiftsgrunnlagService.validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(skatteforholdTilNorgeRequest, innvilgedeMedlemskapsperioder)
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingsresultatID)
            .map { it.fastsattTrygdeavgift?.trygdeavgiftsperioder }
            .orElse(null)
            ?: emptySet()
    }

    @Transactional(readOnly = true)
    fun finnFakturamottakerNavn(behandlingID: Long): String {
        val fagsak = behandlingService.hentBehandling(behandlingID).fagsak
        return fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
            .map {
                if (it.erPerson())
                    persondataService.hentSammensattNavn(it.personIdent)
                else
                    eregFasade.hentOrganisasjonNavn(it.orgnr)
            }
            .orElse(persondataService.hentSammensattNavn(fagsak.hentBruker().aktørId))
    }
}
