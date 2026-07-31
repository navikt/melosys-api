package no.nav.melosys.service.placeholder

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.kodeverk.KodeverkService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.jvm.optionals.getOrNull

/**
 * Egen bean slik at transaksjonen faktisk lukkes før persondata- og EREG-oppslagene: et internt
 * kall i PlaceholderService ville gått utenom Spring-proxyen. Alt som er bundet til databasen
 * materialiseres her, som rene dataklasser – ingen entiteter forlater transaksjonen.
 */
@Component
class PlaceholderSakskontekstHenter(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val landvelgerService: LandvelgerService,
    private val avklarteVirksomheterService: AvklarteVirksomheterService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val landnavnOppslag: PlaceholderLandnavnOppslag,
) {

    @Transactional(readOnly = true)
    fun hent(behandlingId: Long): PlaceholderSakskontekst {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId)
        val fagsak = behandling.fagsak
        val behandlingsresultat = delfelt(behandlingId, "behandlingsresultat") {
            behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(behandlingId)
        }
        return PlaceholderSakskontekst(
            saksnummer = fagsak.saksnummer,
            brukersAktørID = fagsak.finnBrukersAktørID(),
            erLovvalg = fagsak.erLovvalg(),
            lovvalgsperiode = behandlingsresultat?.let { lovvalgsperiode(behandlingId, it) },
            medlemskapsperiodeFom = behandlingsresultat?.let {
                delfelt(behandlingId, "medlemskapsperiode fra") { it.utledAvgiftspliktigperioderFom() }
            },
            medlemskapsperiodeTom = behandlingsresultat?.let {
                delfelt(behandlingId, "medlemskapsperiode til") { it.utledAvgiftspliktigperioderTom() }
            },
            avgiftspliktigPerioder = behandlingsresultat?.let { avgiftspliktigPerioder(behandlingId, it) }.orEmpty(),
            soknadsperioder = soknadsperioder(behandlingId, behandling),
            arbeidsland = arbeidsland(behandlingId),
            utenlandskeArbeidsgivere = utenlandskeArbeidsgivere(behandlingId, behandling),
            norskeArbeidsgivereOrgnumre = norskeArbeidsgivereOrgnumre(behandlingId, behandling),
            fakta = betingelseFakta(behandlingId, behandling, behandlingsresultat),
        )
    }

    /** Betingelsene beregnes ferdig til Boolean her, i transaksjonen: alt under Behandlingsresultat er lazy. */
    private fun betingelseFakta(behandlingId: Long, behandling: Behandling, behandlingsresultat: Behandlingsresultat?) =
        BetingelseFakta(
            erInnvilgelse = fraResultat(behandlingId, behandlingsresultat, "innvilgelse") { it.erInnvilgelse() },
            erAvslag = fraResultat(behandlingId, behandlingsresultat, "avslag") { it.erAvslag() },
            erOpphørt = fraResultat(behandlingId, behandlingsresultat, "opphørt") { it.erOpphørt() },
            // finnAnmodningsperiode() kaster ved flere perioder – delfeltet fanger, og betingelsen utelates
            erDelvisInnvilgelse = fraResultat(behandlingId, behandlingsresultat, "delvis innvilgelse") { resultat ->
                resultat.finnAnmodningsperiode()
                    .map { it.anmodningsperiodeSvar?.erGyldigDelvisInnvilgelse() == true }
                    .orElse(false)
            },
            harÅpenSluttdato = fraResultat(behandlingId, behandlingsresultat, "åpen sluttdato") {
                it.utledAvgiftspliktigperioderTom() == null
            },
            // Ukjent skatteplikttype er ikke det samme som «ikke skattepliktig» – da utelates betingelsen
            erSkattepliktig = fraResultat(behandlingId, behandlingsresultat, "skattepliktig") { resultat ->
                resultat.utledSkatteplikttype()?.let { it == Skatteplikttype.SKATTEPLIKTIG }
            },
            harLønnFraNorge = fraResultat(behandlingId, behandlingsresultat, "lønn fra Norge") { resultat ->
                resultat.hentInntektsperioder().any { it.type == Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE }
            },
            harInntektFraUtlandet = fraResultat(behandlingId, behandlingsresultat, "inntekt fra utlandet") { resultat ->
                resultat.hentInntektsperioder().any { it.type == Inntektskildetype.INNTEKT_FRA_UTLANDET }
            },
            // Ren beregning over trygdeavgiftsperiodene; uten perioder har mottakeren ingen mening
            trygdeavgiftTilSkatt = fraResultat(behandlingId, behandlingsresultat, "trygdeavgift til skatt") { resultat ->
                resultat.trygdeavgiftsperioder.toList().takeIf { it.isNotEmpty() }?.let {
                    trygdeavgiftMottakerService.getTrygdeavgiftMottaker(it) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
                }
            },
            erUtsending = behandling.erUtsending(),
            erPensjonist = behandling.erPensjonist(),
            erFørstegangsvurdering = behandling.erFørstegangsvurdering(),
            erNyVurdering = behandling.erNyVurdering(),
        )

    private fun <T> fraResultat(
        behandlingId: Long,
        behandlingsresultat: Behandlingsresultat?,
        navn: String,
        oppslag: (Behandlingsresultat) -> T,
    ): T? = behandlingsresultat?.let { resultat -> delfelt(behandlingId, navn) { oppslag(resultat) } }

    private fun lovvalgsperiode(behandlingId: Long, behandlingsresultat: Behandlingsresultat): PeriodeData? =
        delfelt(behandlingId, "lovvalgsperiode") { behandlingsresultat.finnLovvalgsperiode().getOrNull()?.let(::periodeData) }

    /** finnAvgiftspliktigPerioder() leser lazy helseutgiftDekkesPerioder for EØS-pensjonister – må skje her, i transaksjonen. */
    private fun avgiftspliktigPerioder(behandlingId: Long, behandlingsresultat: Behandlingsresultat): List<PeriodeData> =
        delfelt(behandlingId, "avgiftspliktige perioder") {
            behandlingsresultat.finnAvgiftspliktigPerioder()
                .map { periodeData(it) }
                .sortedWith(compareByDescending(nullsFirst<LocalDate>()) { it.fom })
        }.orEmpty()

    /** Behandlingens periode er forhåndsvalget, og søknadens samlede utsendingsperiode er alternativet når den er ulik. */
    private fun soknadsperioder(behandlingId: Long, behandling: Behandling): List<PeriodeData> =
        delfelt(behandlingId, "søknadsperiode") {
            val fraBehandling = behandling.finnPeriode().getOrNull()?.let(::periodeData)
            val fraSøknad = (behandling.mottatteOpplysninger?.mottatteOpplysningerData as? Soeknad)
                ?.utenlandsoppdraget?.samletUtsendingsperiode?.let(::periodeData)
            listOfNotNull(fraBehandling, fraSøknad).filter { it.fom != null || it.tom != null }.distinct()
        }.orEmpty()

    private fun arbeidsland(behandlingId: Long): List<String> = delfelt(behandlingId, "arbeidsland") {
        landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingId)
            // Landvelgeren svarer med et HashSet av enum – sorteres så forhåndsvalget ikke varierer mellom JVM-er
            .sortedBy { it.kode }
            .map { landnavnOppslag.landnavn(it.kode) }
            .filter { it.isNotEmpty() }
    }.orEmpty()

    // De to arbeidsgiveroppslagene henter hver sin kopi av de avklarte orgnumrene; gjenbruk krever endring i AvklarteVirksomheterService
    private fun utenlandskeArbeidsgivere(behandlingId: Long, behandling: Behandling): List<String> =
        delfelt(behandlingId, "utenlandske arbeidsgivere") {
            avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling).mapNotNull { it.navn }
        }.orEmpty()

    private fun norskeArbeidsgivereOrgnumre(behandlingId: Long, behandling: Behandling): Set<String> =
        delfelt(behandlingId, "norske arbeidsgivere") {
            avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)
        }.orEmpty()

    private fun periodeData(periode: ErPeriode): PeriodeData =
        PeriodeData(fom = periode.fom, tom = periode.tom, erInnvilget = (periode as? AvgiftspliktigPeriode)?.erInnvilget() == true)

    // Et delfelt som feiler skal ikke velte de øvrige – samme filosofi som feilhåndteringen per resolver
    private fun <T> delfelt(behandlingId: Long, navn: String, oppslag: () -> T): T? =
        try {
            oppslag()
        } catch (e: Exception) {
            log.debug(
                "Placeholder: fant ikke {} for behandling {} ({}), feltene utelates",
                navn, behandlingId, e.javaClass.simpleName,
            )
            null
        }

    private companion object {
        private val log = LoggerFactory.getLogger(PlaceholderSakskontekstHenter::class.java)
    }
}

/** Samme fallback-kjede som DokgenMapperDatahenter.hentLandnavnFraLandkode: ISO2 først, så Landkoder, tom streng ved ukjent. */
@Component
class PlaceholderLandnavnOppslag(private val kodeverkService: KodeverkService) {

    fun landnavn(landkode: String?): String {
        if (landkode.isNullOrBlank()) return ""
        val landnavn = kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, landkode)
            .takeIf { it != KodeverkService.UKJENT }
            ?: kodeverkService.dekod(FellesKodeverk.LANDKODER, landkode)
        return if (landnavn == KodeverkService.UKJENT) "" else landnavn
    }
}

data class PlaceholderSakskontekst(
    val saksnummer: String,
    val brukersAktørID: String?,
    val erLovvalg: Boolean = false,
    val lovvalgsperiode: PeriodeData? = null,
    val medlemskapsperiodeFom: LocalDate? = null,
    val medlemskapsperiodeTom: LocalDate? = null,
    val avgiftspliktigPerioder: List<PeriodeData> = emptyList(),
    /** Første periode er forhåndsvalget, resten er alternativer – fra og til leses alltid fra samme periode. */
    val soknadsperioder: List<PeriodeData> = emptyList(),
    val arbeidsland: List<String> = emptyList(),
    val utenlandskeArbeidsgivere: List<String> = emptyList(),
    val norskeArbeidsgivereOrgnumre: Set<String> = emptySet(),
    val fakta: BetingelseFakta = BetingelseFakta(),
) {
    /** Brev-listenes konvensjon: bare innvilgede perioder, i rekkefølgen henteren materialiserte dem (nyeste først). */
    fun innvilgedePerioder(): List<PeriodeData> = avgiftspliktigPerioder.filter { it.erInnvilget }

    /** Null for lovvalgssaker: der er de avgiftspliktige periodene lovvalgsperioder, som lovvalgsnøklene allerede dekker. */
    fun medlemskapsperioder(): List<PeriodeData>? = if (erLovvalg) null else innvilgedePerioder()
}

/** De ferdig beregnede fakta betingelsene i registeret svarer på. Null betyr utilgjengelig for behandlingen. */
data class BetingelseFakta(
    val erInnvilgelse: Boolean? = null,
    val erAvslag: Boolean? = null,
    val erOpphørt: Boolean? = null,
    val erDelvisInnvilgelse: Boolean? = null,
    val harÅpenSluttdato: Boolean? = null,
    val erSkattepliktig: Boolean? = null,
    val harLønnFraNorge: Boolean? = null,
    val harInntektFraUtlandet: Boolean? = null,
    val trygdeavgiftTilSkatt: Boolean? = null,
    val erUtsending: Boolean? = null,
    val erPensjonist: Boolean? = null,
    val erFørstegangsvurdering: Boolean? = null,
    val erNyVurdering: Boolean? = null,
)

data class PeriodeData(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val erInnvilget: Boolean = false,
)
