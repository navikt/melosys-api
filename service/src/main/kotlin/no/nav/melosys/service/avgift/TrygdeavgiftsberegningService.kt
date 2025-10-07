package no.nav.melosys.service.avgift

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import no.nav.melosys.service.avgift.model.InntektsperiodeModel
import no.nav.melosys.service.avgift.model.SkatteforholdTilNorgeModel
import no.nav.melosys.service.avgift.model.TrygdeavgiftsgrunnlagModel
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
    private val trygdeavgiftperiodeErstatter: TrygdeavgiftperiodeErstatter,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val persondataService: PersondataService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer,
    private val unleash: Unleash
) {

    @Transactional(readOnly = true)
    fun beregnOgLagreTrygdeavgift(
        behandlingID: Long,
        skatteforholdsperioder: List<SkatteforholdTilNorge> = emptyList(),
        inntektsperioder: List<Inntektsperiode> = emptyList(),
        dagensDato: LocalDate = LocalDate.now()
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        TrygdeavgiftsberegningValidator.validerForTrygdeavgiftberegning(
            behandlingsresultat,
            skatteforholdsperioder,
            inntektsperioder,
            unleash,
            dagensDato,
        )

        val nyeTrygdeavgiftsperioder =
            lagNyeTrygeavgiftsperioder(behandlingsresultat, skatteforholdsperioder, inntektsperioder, dagensDato)
        trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(behandlingID, nyeTrygdeavgiftsperioder)

        return nyeTrygdeavgiftsperioder.toSet()
    }

    @Suppress("SpringTransactionalMethodCallsInspection") // warning på beregnTrygdeavgift ignoreres pga eksisterende transaksjon
    private fun lagNyeTrygeavgiftsperioder(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        dagensDato: LocalDate = LocalDate.now()
    ): List<Trygdeavgiftsperiode> {
        if (erPliktigMedlemskapSkattepliktig(
                skatteforholdsperioder,
                inntektsperioder,
                behandlingsresultat.medlemskapsperioder
            )
        ) {
            require(behandlingsresultat.medlemskapsperioder.size == 1 && skatteforholdsperioder.size == 1) { "Det skal ikke være flere enn en medlem- og skatteforholdsperiode når medlemskapet er pliktig og skattepliktig" }
            return skattepliktigTrygdeavgiftsperioderAvMedlemskapsperioder(behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() })
        }

        val nyeTrygdeavgiftsperioder = beregnTrygdeavgift(behandlingsresultat, skatteforholdsperioder, inntektsperioder, dagensDato)
        sjekkTrygdeavgiftSkalBetalesTilNav(nyeTrygdeavgiftsperioder)

        return nyeTrygdeavgiftsperioder
    }

    /*
        Vi kan benytte medlemskapsperioden til å opprette trygdeavgiftsperiode med et skatteforhold med samme periode som medlemskapet
        når det er pliktig medlemskap og Skattepliktig Ja.
    */
    private fun skattepliktigTrygdeavgiftsperioderAvMedlemskapsperioder(
        medlemskapsperioder: Collection<Medlemskapsperiode>
    ): List<Trygdeavgiftsperiode> = medlemskapsperioder.map { mp -> opprettSkattepliktigTrygdeavgiftsperiode(mp) }

    private fun opprettSkattepliktigTrygdeavgiftsperiode(medlemskapsperiode: Medlemskapsperiode): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode(
            periodeFra = medlemskapsperiode.fom,
            periodeTil = medlemskapsperiode.tom,
            trygdesats = BigDecimal.ZERO,
            trygdeavgiftsbeløpMd = Penger(BigDecimal.ZERO),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                fomDato = medlemskapsperiode.fom
                tomDato = medlemskapsperiode.tom
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }

        )
    }

    private fun erPliktigMedlemskapSkattepliktig(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsPerioder: List<Inntektsperiode>,
        medlemskapsperioder: Collection<Medlemskapsperiode>
    ): Boolean {
        val erPliktigMedlemskap = medlemskapsperioder
            .filter { it.erInnvilget() }
            .all { it.erPliktig() }

        val inntektskilderErTomt = inntektsPerioder.isEmpty()
        val alleSkatteforholdErSkattepliktige =
            skatteforholdsperioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        return erPliktigMedlemskap && inntektskilderErTomt && alleSkatteforholdErSkattepliktige
    }

    @Transactional(readOnly = true, noRollbackFor = [Throwable::class])
    fun beregnTrygdeavgift(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsperioder: List<Inntektsperiode>,
        dagensDato: LocalDate = LocalDate.now()
    ): List<Trygdeavgiftsperiode> {
        // UUID brukes til å identifisere periodene som danner grunnlag for trygdeavgiftsberegningen
        val inntektsperioderMedUUID = inntektsperioder.map { UUID.randomUUID() to it }
        val skatteforholdsperioderMedUUID = skatteforholdsperioder.map { UUID.randomUUID() to it }

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }
        val skatteforholdsperiodeDtoSet =
            skatteforholdsperioderMedUUID.map { it.second.tilSkatteforholdDto(it.first) }.toSet()
        val inntektsperiodeDtoList = inntektsperioderMedUUID.map { it.second.tilInntektsperiodeDto(it.first) }
        val foedselDato = hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultat.id, innvilgedeMedlemskapsperioder)

        val beregnetTrygdeavgiftList = trygdeavgiftConsumer.beregnTrygdeavgift(
            TrygdeavgiftsberegningRequest(
                innvilgedeMedlemskapsperioder.tilMedlemskapsperiodeDtoSet(),
                skatteforholdsperiodeDtoSet,
                inntektsperiodeDtoList,
                foedselDato
            )
        )

        return beregnetTrygdeavgiftList.map { beregnetAvgiftPerPeriode ->
            lagTrygdeavgiftsperiode(
                beregnetAvgiftPerPeriode,
                skatteforholdsperioderMedUUID,
                inntektsperioderMedUUID,
                behandlingsresultat,
                dagensDato
            )
        }
    }

    fun lagTrygdeavgiftsperiode(
        response: TrygdeavgiftsberegningResponse,
        skatteforholdsperioderMedUUID: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioderMedUUID: List<Pair<UUID, Inntektsperiode>>,
        behandlingsresultat: Behandlingsresultat,
        dagensDato: LocalDate = LocalDate.now()
    ): Trygdeavgiftsperiode {
        val medlemskapsperiodeID = response.grunnlag.medlemskapsperiodeId
        val skatteforholdsperiodeID = response.grunnlag.skatteforholdsperiodeId
        val inntektsperiodeID = response.grunnlag.inntektsperiodeId


        val grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder
            .firstOrNull { idToUUid(it.id) == medlemskapsperiodeID }
            ?: throw IllegalStateException("Fant ikke medlemskapsperiode $medlemskapsperiodeID")

        val grunnlagSkatteforholdTilNorge = skatteforholdsperioderMedUUID
            .find { it.first == skatteforholdsperiodeID }?.second
            ?: throw IllegalStateException("Fant ikke skatteforholdsperiode $skatteforholdsperiodeID")

        val grunnlagInntekstperiode = inntektsperioderMedUUID
            .find { it.first == inntektsperiodeID }?.second
            ?: throw IllegalStateException("Fant ikke inntektsperiode $inntektsperiodeID")

        return Trygdeavgiftsperiode(
            periodeFra = response.beregnetPeriode.periode.fom,
            periodeTil = response.beregnetPeriode.periode.tom,
            trygdesats = response.beregnetPeriode.sats,
            trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger(),
            grunnlagMedlemskapsperiode = grunnlagMedlemskapsperiode,
            grunnlagSkatteforholdTilNorge = grunnlagSkatteforholdTilNorge,
            grunnlagInntekstperiode = grunnlagInntekstperiode,
            forskuddsvisFaktura = if (unleash.isEnabled(MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
                response.beregnetPeriode.periode.fom.year >= dagensDato.year
            } else {
                true
            }
        )
    }

    private fun hentFødselsdatoOmViHarTjenstligBehov(
        behandlingsresultatID: Long,
        medlemskapsperioder: List<Medlemskapsperiode>
    ): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato
        }
        return null
    }

    private fun sjekkTrygdeavgiftSkalBetalesTilNav(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {
        val erAlleTrygdeavgiftNullBeløp =
            trygdeavgiftsperioder.all { it.trygdeavgiftsbeløpMd.verdi.compareTo(BigDecimal.ZERO) == 0 }

        val skalKunBetalesTilSkatt = trygdeavgiftMottakerService
            .getTrygdeavgiftMottaker(trygdeavgiftsperioder) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT

        check(erAlleTrygdeavgiftNullBeløp || !skalKunBetalesTilSkatt) { "Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0." }
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
            .trygdeavgiftsperioder
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegningForEosPensjonist(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
            .eøsPensjonistTrygdeavgiftsperioder
    }

    @Transactional(readOnly = true)
    fun hentOpprinneligTrygdeavgiftsperioder(behandlingID: Long): TrygdeavgiftsgrunnlagModel {
        val behandling = behandlingService.hentBehandling(behandlingID)

        if (behandling.type != Behandlingstyper.NY_VURDERING) {
            throw IllegalStateException("Behandling med id $behandlingID er ikke av type ${Behandlingstyper.NY_VURDERING}")
        }

        val opprinneligeTrygdeavgiftsperioder = behandling.opprinneligBehandling?.let {
            behandlingsresultatService.hentBehandlingsresultat(it.id).trygdeavgiftsperioder
        } ?: emptySet()

        val skatteforholdsperioder = opprinneligeTrygdeavgiftsperioder
            .mapNotNull { it.grunnlagSkatteforholdTilNorge }
            .distinctBy { it.id }

        val inntektsperioder = opprinneligeTrygdeavgiftsperioder
            .mapNotNull { it.grunnlagInntekstperiode }
            .distinctBy { it.id }

        val skalJusterePerioder = unleash.isEnabled(MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)
        val inneværendeÅr = LocalDate.now().year
        val førsteJanuar = if (skalJusterePerioder) LocalDate.now().withDayOfYear(1) else null

        return TrygdeavgiftsgrunnlagModel(
            skatteforholdsperioder
                .filter { !skalJusterePerioder || it.tom.year >= inneværendeÅr }
                .map { SkatteforholdTilNorgeModel(it, førsteJanuar) },
            inntektsperioder
                .filter { !skalJusterePerioder || it.tom.year >= inneværendeÅr }
                .map { InntektsperiodeModel(it, førsteJanuar) }
        )
    }

    // Metoden ser ikke ut til å høre hjemme her
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
}
