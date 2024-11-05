package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class TrygdeavgiftsberegningService(
    private val behandlingService: BehandlingService,
    private val eregFasade: EregFasade,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val persondataService: PersondataService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer,
) {

    @Transactional
    fun beregnOgLagreTrygdeavgift(
        behandlingsresultatID: Long,
        skatteforholdsperioder: List<SkatteforholdTilNorge> = emptyList(),
        inntektsperioder: List<Inntektsperiode> = emptyList(),
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        nullstillTrygdeavgiftsperioder(behandlingsresultat)
        TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(behandlingsresultat, skatteforholdsperioder, inntektsperioder)

        return if (erPliktigMedlemskapSkattepliktig(skatteforholdsperioder, inntektsperioder, behandlingsresultat)) {
            leggTilNyeTrygdeavgiftsperioderForPliktigMedlemskapSkattepliktig(skatteforholdsperioder, behandlingsresultat)
        } else {
            val inntektsperioderPair = inntektsperioder.map { Pair(UUID.randomUUID(), it) }
            val inntektsperiodeDtos = inntektsperioderPair.map { it.second.tilInntektsperiodeDto(it.first) }

            val skatteforholdsperioderPair = skatteforholdsperioder.map { Pair(UUID.randomUUID(), it) }
            val skatteforholdsperioderDtos = skatteforholdsperioderPair.map { it.second.tilSkatteforholdDto(it.first) }

            val beregnetTrygdeavgift = beregnTrygdeAvgift(behandlingsresultat, skatteforholdsperioderDtos, inntektsperiodeDtos)

            val nyeTrygdeavgiftsperioder = beregnetTrygdeavgift.map { response ->
                lagTrygdeavgiftsperiode(response, skatteforholdsperioderPair, inntektsperioderPair, behandlingsresultat)
            }.toSet()

            nyeTrygdeavgiftsperioder.also {
                validerTrygdeavgiftBetalesTilNav(behandlingsresultat, beregnetTrygdeavgift)
                behandlingsresultatService.lagreOgFlush(behandlingsresultat)
            }
        }
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
            .trygdeavgiftsperioder
    }

    @Transactional(readOnly = true)
    fun hentOpprinneligTrygdeavgiftsperioder(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val behandling = behandlingsresultat.behandling
        behandling.opprinneligBehandling?.let {
            return behandlingsresultatService.hentBehandlingsresultat(it.id).trygdeavgiftsperioder
                ?: emptySet()
        }
        return emptySet()
    }

    @Transactional(readOnly = true)
    fun finnFakturamottakerNavn(behandlingID: Long): String {
        val fagsak = behandlingService.hentBehandling(behandlingID).fagsak
        fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
            .let {
                if (it == null)
                    return persondataService.hentSammensattNavn(fagsak.hentBrukersAktørID())
                if (it.erPerson())
                    return persondataService.hentSammensattNavn(it.personIdent)
                return eregFasade.hentOrganisasjonNavn(it.orgnr)
            }
    }

    private fun lagTrygdeavgiftsperiode(
        response: TrygdeavgiftsberegningResponse,
        skatteforholdsperioder2Pair: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioder2Pair: List<Pair<UUID, Inntektsperiode>>,
        behandlingsresultat: Behandlingsresultat
    ) = Trygdeavgiftsperiode().apply {
        periodeFra = response.beregnetPeriode.periode.fom
        periodeTil = response.beregnetPeriode.periode.tom
        trygdesats = response.beregnetPeriode.sats
        trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger()
        grunnlagSkatteforholdTilNorge = skatteforholdsperioder2Pair.find { it.first == response.grunnlag.skatteforholdsperiodeId }?.second
        grunnlagInntekstperiode = inntektsperioder2Pair.find { it.first == response.grunnlag.inntektsperiodeId }?.second
        grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder.first { idToUUid(it.id) == response.grunnlag.medlemskapsperiodeId }
            ?: throw IllegalStateException("Fant ikke medlemskapsperiode")
        grunnlagMedlemskapsperiode.trygdeavgiftsperioder.add(this)
    }

    private fun beregnTrygdeAvgift(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioderTemp: List<SkatteforholdsperiodeDto>,
        inntektsPerioderTemp: List<InntektsperiodeDto>
    ): List<TrygdeavgiftsberegningResponse> {
        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }
        val medlemskapsperiodeDtos = innvilgedeMedlemskapsperioder.tilMedlemskapsperiodeDtos()
        val foedselDato = hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultat.id, innvilgedeMedlemskapsperioder)


        val beregnetTrygdeavgift = beregnTrygdeAvgift(medlemskapsperiodeDtos, skatteforholdsperioderTemp.toSet(), inntektsPerioderTemp, foedselDato)

        return beregnetTrygdeavgift
    }

    private fun erPliktigMedlemskapSkattepliktig(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsPerioder: List<Inntektsperiode>,
        behandlingsresultat: Behandlingsresultat
    ): Boolean {
        val erPliktigMedlemskap = behandlingsresultat.medlemskapsperioder
            .filter { it.erInnvilget() }
            .all { it.erPliktig() }

        val inntektskilderErTomt = inntektsPerioder.isEmpty()
        val alleSkatteforholdErSkattepliktige =
            skatteforholdsperioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        return erPliktigMedlemskap && inntektskilderErTomt && alleSkatteforholdErSkattepliktige
    }

    private fun nullstillTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.medlemskapsperioder.forEach {
            it.trygdeavgiftsperioder.clear()
        }

        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }

    private fun leggTilNyeTrygdeavgiftsperioderForPliktigMedlemskapSkattepliktig(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        behandlingsresultat: Behandlingsresultat,
    ): Set<Trygdeavgiftsperiode> {
        check(skatteforholdsperioder.size == 1) { "Det skal ikke være flere enn en skatteforholdsperiode når medlemskapet er pliktig og skattepliktig" }

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }

        return innvilgedeMedlemskapsperioder.map {
            val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
                fomDato = skatteforholdsperioder.first().fom
                tomDato = skatteforholdsperioder.first().tom
                skatteplikttype = skatteforholdsperioder.first().skatteplikttype
            }
            val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
                periodeFra = it.fom
                periodeTil = it.tom
                trygdesats = BigDecimal.ZERO
                trygdeavgiftsbeløpMd = Penger(BigDecimal.ZERO)
                grunnlagMedlemskapsperiode = it
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge
            }
            it.trygdeavgiftsperioder.add(trygdeavgiftsperiode)

            trygdeavgiftsperiode
        }.toSet()
    }

    private fun beregnTrygdeAvgift(
        medlemskapsperiodeDtos: Set<MedlemskapsperiodeDto>,
        skatteforholdsperioderDtos: Set<SkatteforholdsperiodeDto>,
        inntektsperioderDtos: List<InntektsperiodeDto>,
        foedselDato: LocalDate?
    ): List<TrygdeavgiftsberegningResponse> {
        val trygdeavgiftsberegningRequest = TrygdeavgiftsberegningRequest(
            medlemskapsperiodeDtos,
            skatteforholdsperioderDtos,
            inntektsperioderDtos,
            foedselDato
        )
        return trygdeavgiftConsumer.beregnTrygdeavgift(trygdeavgiftsberegningRequest)
    }

    private fun hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID: Long, medlemskapsperioder: List<Medlemskapsperiode>): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato
        }
        return null
    }

    private fun validerTrygdeavgiftBetalesTilNav(
        behandlingsresultat: Behandlingsresultat,
        beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>
    ) {
        val erAlleTrygdeavgiftbelopNull = beregnetTrygdeavgift.all { it.beregnetPeriode.månedsavgift.verdi.compareTo(BigDecimal.ZERO) == 0 }
        val skalKunBetalesTilSkatt =
            trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
        check(!(skalKunBetalesTilSkatt && !erAlleTrygdeavgiftbelopNull)) { "Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0." }
    }
}
