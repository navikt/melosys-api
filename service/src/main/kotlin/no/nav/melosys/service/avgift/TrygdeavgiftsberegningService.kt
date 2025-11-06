package no.nav.melosys.service.avgift

import io.getunleash.Unleash
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import no.nav.melosys.service.avgift.model.InntektsperiodeModel
import no.nav.melosys.service.avgift.model.SkatteforholdTilNorgeModel
import no.nav.melosys.service.avgift.model.TrygdeavgiftsgrunnlagModel
import no.nav.melosys.service.avgift.tilMedlemskapsperiodeDtoSet
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
                behandlingsresultat.avgiftspliktigPerioder()
            )
        ) {
            require(behandlingsresultat.avgiftspliktigPerioder().size == 1 && skatteforholdsperioder.size == 1) { "Det skal ikke være flere enn en avgiftspliktig- og skatteforholdsperiode når perioden er pliktig og skattepliktig" }
            return skattepliktigTrygdeavgiftsperioderAvAvgiftspliktigperioder(behandlingsresultat.avgiftspliktigPerioder().filter { it.erInnvilget() })
        }

        val nyeTrygdeavgiftsperioder = beregnTrygdeavgift(behandlingsresultat, skatteforholdsperioder, inntektsperioder, dagensDato)
        sjekkTrygdeavgiftSkalBetalesTilNav(nyeTrygdeavgiftsperioder)

        return nyeTrygdeavgiftsperioder
    }

    /*
        Vi kan benytte medlemskapsperioden til å opprette trygdeavgiftsperiode med et skatteforhold med samme periode som medlemskapet
        når det er pliktig medlemskap og Skattepliktig Ja.
    */
    private fun skattepliktigTrygdeavgiftsperioderAvAvgiftspliktigperioder(
        avgiftspliktigperioder: Collection<AvgiftspliktigPeriode>
    ): List<Trygdeavgiftsperiode> = avgiftspliktigperioder.map { mp -> opprettSkattepliktigTrygdeavgiftsperiode(mp) }

    private fun opprettSkattepliktigTrygdeavgiftsperiode(avgiftspliktigperiode: AvgiftspliktigPeriode): Trygdeavgiftsperiode {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = avgiftspliktigperiode.fom,
            periodeTil = avgiftspliktigperiode.tom,
            trygdesats = BigDecimal.ZERO,
            trygdeavgiftsbeløpMd = Penger(BigDecimal.ZERO),
            grunnlagSkatteforholdTilNorge = SkatteforholdTilNorge().apply {
                fomDato = avgiftspliktigperiode.fom
                tomDato = avgiftspliktigperiode.tom
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        when (avgiftspliktigperiode) {
            is Medlemskapsperiode -> trygdeavgiftsperiode.apply { grunnlagMedlemskapsperiode = avgiftspliktigperiode }
            is HelseutgiftDekkesPeriode -> trygdeavgiftsperiode.apply { grunnlagHelseutgiftDekkesPeriode = avgiftspliktigperiode }
            is Lovvalgsperiode -> trygdeavgiftsperiode.apply { grunnlagLovvalgsPeriode = avgiftspliktigperiode }
            else -> throw FunksjonellException("Ukjent avgiftspliktigperiode: ${avgiftspliktigperiode::class.java.simpleName}")
        }

        return trygdeavgiftsperiode
    }

    private fun erPliktigMedlemskapSkattepliktig(
        skatteforholdsperioder: List<SkatteforholdTilNorge>,
        inntektsPerioder: List<Inntektsperiode>,
        avgiftspliktigePerioder: Collection<AvgiftspliktigPeriode>
    ): Boolean {
        val erPliktig = avgiftspliktigePerioder
            .filter { it.erInnvilget() }
            .all { it.erPliktig() }

        val inntektskilderErTomt = inntektsPerioder.isEmpty()
        val alleSkatteforholdErSkattepliktige =
            skatteforholdsperioder.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        return erPliktig && inntektskilderErTomt && alleSkatteforholdErSkattepliktige
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

        val innvilgedeAvgiftspliktigperioder = behandlingsresultat.avgiftspliktigPerioder().filter { it.erInnvilget() }
        val skatteforholdsperiodeDtoSet =
            skatteforholdsperioderMedUUID.map { it.second.tilSkatteforholdDto(it.first) }.toSet()
        val inntektsperiodeDtoList = inntektsperioderMedUUID.map { it.second.tilInntektsperiodeDto(it.first) }
        val foedselDato = hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultat.hentId(), innvilgedeAvgiftspliktigperioder)

        val beregnetTrygdeavgiftList = trygdeavgiftConsumer.beregnTrygdeavgift(
            TrygdeavgiftsberegningRequest(
                innvilgedeAvgiftspliktigperioder.tilMedlemskapsperiodeDtoSet(),
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
        val skatteforholdsperiodeID = response.grunnlag.skatteforholdsperiodeId
        val inntektsperiodeID = response.grunnlag.inntektsperiodeId

        val grunnlagSkatteforholdTilNorge = skatteforholdsperioderMedUUID
            .find { it.first == skatteforholdsperiodeID }?.second
            ?: throw IllegalStateException("Fant ikke skatteforholdsperiode $skatteforholdsperiodeID")

        val grunnlagInntekstperiode = inntektsperioderMedUUID
            .find { it.first == inntektsperiodeID }?.second
            ?: throw IllegalStateException("Fant ikke inntektsperiode $inntektsperiodeID")

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = response.beregnetPeriode.periode.fom,
            periodeTil = response.beregnetPeriode.periode.tom,
            trygdesats = response.beregnetPeriode.sats,
            trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger(),
            grunnlagSkatteforholdTilNorge = grunnlagSkatteforholdTilNorge,
            grunnlagInntekstperiode = grunnlagInntekstperiode,
            forskuddsvisFaktura = if (unleash.isEnabled(MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
                response.beregnetPeriode.periode.fom.year >= dagensDato.year
            } else {
                true
            }
        )

        when (behandlingsresultat.avgiftspliktigPerioder().firstOrNull()) {
            is Medlemskapsperiode -> trygdeavgiftsperiode.apply {
                grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder.firstOrNull { idToUUid(it.hentId()) == response.grunnlag.medlemskapsperiodeId } ?: throw IllegalStateException("Fant ikke medlemskapsperiode ${response.grunnlag.medlemskapsperiodeId}")
            }
            is HelseutgiftDekkesPeriode -> trygdeavgiftsperiode.apply {
                grunnlagHelseutgiftDekkesPeriode = behandlingsresultat.hentHelseutgiftDekkesPeriode().takeIf { idToUUid(it.hentId()) == response.grunnlag.medlemskapsperiodeId } ?: throw IllegalStateException("Fant ikke helseutgiftdekket periode ${response.grunnlag.medlemskapsperiodeId}")
            }
            is Lovvalgsperiode -> trygdeavgiftsperiode.apply {
                grunnlagLovvalgsPeriode = behandlingsresultat.lovvalgsperioder.firstOrNull { idToUUid(it.hentId()) == response.grunnlag.medlemskapsperiodeId } ?: throw IllegalStateException("Fant ikke lovvalgsperiode ${response.grunnlag.medlemskapsperiodeId}")
            }
            else -> throw FunksjonellException("Ukjent avgiftspliktigperiode: ${behandlingsresultat.avgiftspliktigPerioder().firstOrNull()}")
        }

        return trygdeavgiftsperiode
    }

    private fun hentFødselsdatoOmViHarTjenstligBehov(
        behandlingsresultatID: Long,
        avgiftspliktigperioder: List<AvgiftspliktigPeriode>
    ): LocalDate? {
        if (avgiftspliktigperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato
        }
        return null
    }

    private fun sjekkTrygdeavgiftSkalBetalesTilNav(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) {
        val erAlleTrygdeavgiftNullBeløp =
            trygdeavgiftsperioder.all { it.trygdeavgiftsbeløpMd.hentVerdi().compareTo(BigDecimal.ZERO) == 0 }

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


        if (unleash.isEnabled(MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER) && behandlingsresultatService.hentBehandlingsresultat(
                behandlingID
            ).medlemskapsperioder.all { it.hentTom().year < LocalDate.now().year }
        ) {
            return TrygdeavgiftsgrunnlagModel(
                emptyList(),
                emptyList()
            )
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
