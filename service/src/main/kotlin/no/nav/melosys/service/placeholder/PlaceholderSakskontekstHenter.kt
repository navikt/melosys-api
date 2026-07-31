package no.nav.melosys.service.placeholder

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.service.LandvelgerService
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
    private val landnavnOppslag: PlaceholderLandnavnOppslag,
) {

    @Transactional(readOnly = true)
    fun hent(behandlingId: Long): PlaceholderSakskontekst {
        val behandling = behandlingService.hentBehandlingMedSaksopplysninger(behandlingId)
        val fagsak = behandling.fagsak
        val behandlingsresultat = delfelt("behandlingsresultat") {
            behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(behandlingId)
        }
        return PlaceholderSakskontekst(
            saksnummer = fagsak.saksnummer,
            brukersAktørID = fagsak.finnBrukersAktørID(),
            lovvalgsperiode = behandlingsresultat?.let { lovvalgsperiode(it) },
            medlemskapsperiodeFom = behandlingsresultat?.let { delfelt("medlemskapsperiode fra") { it.utledAvgiftspliktigperioderFom() } },
            medlemskapsperiodeTom = behandlingsresultat?.let { delfelt("medlemskapsperiode til") { it.utledAvgiftspliktigperioderTom() } },
            avgiftspliktigPerioder = behandlingsresultat?.let { avgiftspliktigPerioder(it) }.orEmpty(),
            soknadsperioder = soknadsperioder(behandling),
            arbeidsland = arbeidsland(behandlingId),
            utenlandskeArbeidsgivere = utenlandskeArbeidsgivere(behandling),
            norskeArbeidsgivereOrgnumre = norskeArbeidsgivereOrgnumre(behandling),
        )
    }

    private fun lovvalgsperiode(behandlingsresultat: Behandlingsresultat): PeriodeData? =
        delfelt("lovvalgsperiode") { behandlingsresultat.finnLovvalgsperiode().getOrNull()?.let(::periodeData) }

    /** finnAvgiftspliktigPerioder() leser lazy helseutgiftDekkesPerioder for EØS-pensjonister – må skje her, i transaksjonen. */
    private fun avgiftspliktigPerioder(behandlingsresultat: Behandlingsresultat): List<PeriodeData> =
        delfelt("avgiftspliktige perioder") {
            behandlingsresultat.finnAvgiftspliktigPerioder()
                .map { periodeData(it) }
                .sortedWith(compareByDescending(nullsFirst<LocalDate>()) { it.fom })
        }.orEmpty()

    private fun soknadsperioder(behandling: Behandling): List<PeriodeData> = delfelt("søknadsperiode") {
        val fraBehandling = behandling.finnPeriode().getOrNull()?.let(::periodeData)
        val fraSøknad = (behandling.mottatteOpplysninger?.mottatteOpplysningerData as? Soeknad)
            ?.utenlandsoppdraget?.samletUtsendingsperiode?.let(::periodeData)
        listOfNotNull(fraBehandling, fraSøknad).filter { it.fom != null || it.tom != null }.distinct()
    }.orEmpty()

    private fun arbeidsland(behandlingId: Long): List<String> = delfelt("arbeidsland") {
        landvelgerService.hentAlleArbeidslandUtenMarginaltArbeid(behandlingId)
            .map { landnavnOppslag.landnavn(it.kode) }
            .filter { it.isNotEmpty() }
    }.orEmpty()

    private fun utenlandskeArbeidsgivere(behandling: Behandling): List<String> = delfelt("utenlandske arbeidsgivere") {
        avklarteVirksomheterService.hentUtenlandskeVirksomheter(behandling).mapNotNull { it.navn }
    }.orEmpty()

    private fun norskeArbeidsgivereOrgnumre(behandling: Behandling): Set<String> = delfelt("norske arbeidsgivere") {
        avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(behandling)
    }.orEmpty()

    private fun periodeData(periode: ErPeriode): PeriodeData =
        PeriodeData(fom = periode.fom, tom = periode.tom, erInnvilget = (periode as? AvgiftspliktigPeriode)?.erInnvilget() == true)

    // Et delfelt som feiler skal ikke velte de øvrige – samme filosofi som feilhåndteringen per resolver
    private fun <T> delfelt(navn: String, oppslag: () -> T): T? =
        try {
            oppslag()
        } catch (e: Exception) {
            log.info("Placeholder: fant ikke {} for saken ({}), feltene utelates", navn, e.javaClass.simpleName)
            null
        }

    private companion object {
        private val log = LoggerFactory.getLogger(PlaceholderSakskontekstHenter::class.java)
    }
}

/** Landnavn med samme fallback-kjede som brevbyggingen: ISO2 først, så Landkoder, tom streng ved ukjent. */
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
    val lovvalgsperiode: PeriodeData? = null,
    val medlemskapsperiodeFom: LocalDate? = null,
    val medlemskapsperiodeTom: LocalDate? = null,
    val avgiftspliktigPerioder: List<PeriodeData> = emptyList(),
    val soknadsperioder: List<PeriodeData> = emptyList(),
    val arbeidsland: List<String> = emptyList(),
    val utenlandskeArbeidsgivere: List<String> = emptyList(),
    val norskeArbeidsgivereOrgnumre: Set<String> = emptySet(),
) {
    /** Brev-listenes konvensjon: bare innvilgede perioder, i rekkefølgen henteren materialiserte dem (nyeste først). */
    fun innvilgedePerioder(): List<PeriodeData> = avgiftspliktigPerioder.filter { it.erInnvilget }
}

data class PeriodeData(
    val fom: LocalDate?,
    val tom: LocalDate?,
    val erInnvilget: Boolean = false,
)
