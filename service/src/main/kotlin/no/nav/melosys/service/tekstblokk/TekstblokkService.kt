package no.nav.melosys.service.tekstblokk

import java.time.Instant
import java.util.Locale

import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkStatus
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.tekstblokk.TekstblokkRepository
import no.nav.melosys.service.bruker.SaksbehandlerService
import org.hibernate.Hibernate
import org.slf4j.LoggerFactory
import org.springframework.data.domain.AuditorAware
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TekstblokkService(
    private val tekstblokkRepository: TekstblokkRepository,
    private val htmlSanitizer: TekstblokkHtmlSanitizer,
    private val saksbehandlerService: SaksbehandlerService,
    private val auditorAware: AuditorAware<String>,
) {

    data class Input(
        val tittel: String,
        val innhold: String,
        val type: TekstblokkType,
        // Null betyr «uendret» ved oppdatering for tags, avgrensninger og status; tom
        // liste er den eksplisitte nullstillingen («gjelder alle» for avgrensningene).
        // Eldre klienter som utelater felter mister dermed ingenting ved en PUT.
        val tags: List<String>? = null,
        val sakstyper: List<Sakstyper>? = null,
        val sakstemaer: List<Sakstemaer>? = null,
        val behandlingstemaer: List<Behandlingstema>? = null,
        // Null betyr «uendret» ved oppdatering. Ved opprettelse og bulk-seeding faller den
        // tilbake på entitetens PUBLISERT, slik at melosys-console er uendret.
        val status: TekstblokkStatus? = null,
    )

    @Transactional(readOnly = true)
    fun hentAlleOversikter(type: TekstblokkType?, inkluderUtkast: Boolean): List<TekstblokkOversikt> {
        val oversikter = tekstblokkRepository.finnOversikt(type, inkluderUtkast)
        if (oversikter.isEmpty()) return oversikter

        val ider = oversikter.map { it.id }
        val tagsPerId = tekstblokkRepository.finnTagsForIds(ider)
            .groupBy({ it[0] as Long }, { it[1] as String })
        val sakstyperPerId = tekstblokkRepository.finnSakstyperForIds(ider)
            .groupBy({ it[0] as Long }, { it[1] as Sakstyper })
        val sakstemaerPerId = tekstblokkRepository.finnSakstemaerForIds(ider)
            .groupBy({ it[0] as Long }, { it[1] as Sakstemaer })
        val behandlingstemaerPerId = tekstblokkRepository.finnBehandlingstemaerForIds(ider)
            .groupBy({ it[0] as Long }, { it[1] as Behandlingstema })
        oversikter.forEach {
            it.tags = tagsPerId[it.id]?.toSet() ?: emptySet()
            it.sakstyper = sakstyperPerId[it.id]?.toSet() ?: emptySet()
            it.sakstemaer = sakstemaerPerId[it.id]?.toSet() ?: emptySet()
            it.behandlingstemaer = behandlingstemaerPerId[it.id]?.toSet() ?: emptySet()
        }
        return oversikter
    }

    @Transactional(readOnly = true)
    fun hent(id: Long, inkluderUtkast: Boolean): Tekstblokk = finnAktiv(id)
        // Utkast skjules for ikke-admin på samme måte som slettede blokker: samme 404.
        .takeIf { inkluderUtkast || it.status != TekstblokkStatus.UTKAST }
        // open-in-view er av, og avgrensningene ligger utenfor EntityGraph-en for å
        // unngå kartesisk produkt – de må derfor lastes her, inne i transaksjonen.
        ?.also {
            Hibernate.initialize(it.sakstyper)
            Hibernate.initialize(it.sakstemaer)
            Hibernate.initialize(it.behandlingstemaer)
        }
        ?: throw IkkeFunnetException("Finner ikke tekstblokk med id $id")

    private fun finnAktiv(id: Long): Tekstblokk = tekstblokkRepository.findByIdAndSlettetDatoIsNull(id)
        .orElseThrow { IkkeFunnetException("Finner ikke tekstblokk med id $id") }

    @Transactional
    fun opprett(input: Input): Tekstblokk {
        val tekstblokk = Tekstblokk()
        populerFraInput(tekstblokk, input)
        return tekstblokkRepository.save(tekstblokk).also {
            log.info("Opprettet {} med id {}: '{}'", it.type, it.id, it.tittel)
        }
    }

    @Transactional
    fun oppdater(id: Long, input: Input): Tekstblokk {
        val tekstblokk = hent(id, inkluderUtkast = true)
        populerFraInput(tekstblokk, input)
        return tekstblokkRepository.save(tekstblokk).also {
            log.info("Endret {} med id {}: '{}'", it.type, it.id, it.tittel)
        }
    }

    /**
     * Publisering er en egen beslutning, ikke en sideeffekt av lagring: et utkast blir
     * synlig for saksbehandlere først når en administrator aktivt slipper det ut.
     */
    @Transactional
    fun publiser(id: Long): Tekstblokk {
        val tekstblokk = hent(id, inkluderUtkast = true)
        tekstblokk.status = TekstblokkStatus.PUBLISERT
        // Auditingen setter identen til publisereren; navnet må følge med, ellers står
        // forrige redaktørs navn igjen ved siden av publisererens ident.
        tekstblokk.endretAvNavn = hentNavnForInnloggetBruker()
        return tekstblokkRepository.save(tekstblokk).also {
            log.info("Publiserte {} med id {}: '{}'", it.type, id, it.tittel)
        }
    }

    /**
     * Soft delete: raden blir liggende med slettetDato satt, og filtreres bort i
     * spørringene. En admin som sletter ved en feil mister dermed ikke innholdet.
     */
    @Transactional
    fun slett(id: Long) {
        // Slettingen rører ikke avgrensningene, så vi hopper over initialiseringen av dem.
        val tekstblokk = finnAktiv(id)
        tekstblokk.slettetDato = Instant.now()
        tekstblokkRepository.save(tekstblokk)
        log.info("Slettet {} med id {}: '{}'", tekstblokk.type, id, tekstblokk.tittel)
    }

    /**
     * Atomisk batch-opprettelse. Brukes fra melosys-console for å seede inn mange
     * blokker samtidig. Enten lagres alle, eller ingen (én transaksjon).
     */
    @Transactional
    fun opprettBulk(inputs: List<Input>): List<Tekstblokk> {
        // Ett navneoppslag for hele bulken, ikke ett per blokk.
        val endretAvNavn = hentNavnForInnloggetBruker()
        return inputs.map { input ->
            val tekstblokk = Tekstblokk()
            populerFraInput(tekstblokk, input, endretAvNavn)
            tekstblokkRepository.save(tekstblokk)
        }.also { log.info("Opprettet {} tekstblokker i bulk", it.size) }
    }

    private fun populerFraInput(
        tekstblokk: Tekstblokk,
        input: Input,
        endretAvNavn: String? = hentNavnForInnloggetBruker(),
    ) {
        tekstblokk.tittel = input.tittel.trim()
        tekstblokk.innhold = htmlSanitizer.saniter(input.innhold) ?: ""
        tekstblokk.type = input.type
        // Utelatt status lar statusen stå: en redigering skal ikke publisere et utkast
        input.status?.let { tekstblokk.status = it }
        tekstblokk.endretAvNavn = endretAvNavn
        // Utelatte tags og avgrensninger lar dem stå, på samme måte som status. Tom liste
        // er den eksplisitte nullstillingen.
        input.tags?.let { tekstblokk.tags.erstattMed(normaliserTags(it)) }
        input.sakstyper?.let { tekstblokk.sakstyper.erstattMed(it) }
        input.sakstemaer?.let { tekstblokk.sakstemaer.erstattMed(it) }
        input.behandlingstemaer?.let { tekstblokk.behandlingstemaer.erstattMed(it) }
    }

    // Hibernate følger endringer på selve samlingen, så den byttes ikke ut.
    private fun <T> MutableSet<T>.erstattMed(nye: Collection<T>) {
        clear()
        addAll(nye)
    }

    // Et feilet navneoppslag skal ikke hindre lagring – frontend viser ident i stedet.
    private fun hentNavnForInnloggetBruker(): String? = runCatching {
        auditorAware.currentAuditor.orElse(null)?.let { saksbehandlerService.finnNavnForIdent(it).orElse(null) }
    }.onFailure { log.warn("Kunne ikke hente navn for innlogget bruker", it) }.getOrNull()

    // Ikke-nullbar med vilje: null betyr «uendret» og håndteres av kalleren – en ny kaller
    // skal ikke kunne sende null hit og forvente tom liste.
    private fun normaliserTags(tags: List<String>): List<String> =
        tags
            .asSequence()
            // Bevar bokstavstørrelse (f.eks. "USA-avtale") og tillat mellomrom i tags.
            // Vi trimmer kun ytterkanter og slår sammen gjentatt blanktegn til ett.
            .map { it.trim().replace(FLERE_BLANKTEGN, " ") }
            .filter { it.isNotBlank() }
            // Unngå nær-duplikater som kun skiller seg i bokstavstørrelse; behold første variant.
            .distinctBy { it.lowercase(Locale.ROOT) }
            .toList()

    private companion object {
        private val log = LoggerFactory.getLogger(TekstblokkService::class.java)
        private val FLERE_BLANKTEGN = Regex("\\s+")
    }
}
