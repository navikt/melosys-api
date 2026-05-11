package no.nav.melosys.saksflyt.steg.soknad

import mu.KotlinLogging
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.service.aktoer.AktoerDto
import no.nav.melosys.service.aktoer.AktoerService
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/**
 * Synkroniserer aktører + kontaktopplysninger fra digital søknad.
 *
 * Hver innsending er en autoritativ erklæring om den representasjonen den dekker:
 * - Match på orgnr → eksisterende aktør oppdateres med spec.fullmakter (ikke union).
 * - Eksisterende virksomhet-fullmektig uten match: slett ved AG-del; fjern kun FULLMEKTIG_SØKNAD ved ren AT-del.
 * - Eksisterende person-fullmektig uten match: slett ved AT-del; ellers behold urørt.
 * - KONTAKTOPPLYSNING speiler virksomhet-fullmektige.
 */
@Service
class DigitalSøknadAktørSynkronisering(
    private val aktoerService: AktoerService,
    private val kontaktopplysningService: KontaktopplysningService,
    private val persondataFasade: PersondataFasade
) {

    @Transactional
    fun synkroniser(fagsak: Fagsak, aktører: AktørerFraSøknad) {
        if (dekkerArbeidsgiverside(aktører.skjemadel)) {
            synkroniserArbeidsgivere(fagsak, aktører.arbeidsgiverOrgnumre)
        }
        synkroniserFullmektige(fagsak, aktører)
    }

    private fun dekkerArbeidsgiverside(skjemadel: Skjemadel): Boolean =
        skjemadel == Skjemadel.ARBEIDSGIVERS_DEL || skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL

    private fun dekkerArbeidstakerside(skjemadel: Skjemadel): Boolean =
        skjemadel == Skjemadel.ARBEIDSTAKERS_DEL || skjemadel == Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL

    private fun synkroniserArbeidsgivere(fagsak: Fagsak, ønskede: List<String>) {
        val eksisterende = aktoerService.hentfagsakAktører(fagsak, Aktoersroller.ARBEIDSGIVER)
            .mapNotNull { it.orgnr }.toSet()
        if (eksisterende == ønskede.toSet()) {
            log.debug { "Arbeidsgiver-aktører uendret på sak ${fagsak.saksnummer}, hopper over" }
            return
        }
        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, ønskede)
    }

    private fun synkroniserFullmektige(fagsak: Fagsak, aktører: AktørerFraSøknad) {
        val eksisterende = aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG)
        val ønskedeIder = aktører.fullmektige.map(::identifikator).toSet()
        val nyeOrgnr = aktører.fullmektige.mapNotNull { it.orgnr }.toSet()

        // 1. Behandle eksisterende uten match — slett eller reduser fullmakter avhengig av side
        val slettedeOrgnr = mutableSetOf<String>()
        eksisterende.forEach { eksAkt ->
            if (identifikator(eksAkt) !in ønskedeIder) {
                val bleSlettet = håndterEksisterendeUtenMatch(fagsak, eksAkt, aktører.skjemadel)
                if (bleSlettet) eksAkt.orgnr?.let { slettedeOrgnr.add(it) }
            }
        }

        // 2. Lagre/oppdatere ønskede fullmektige + tilhørende kontaktopplysninger
        aktører.fullmektige.forEach { spec ->
            val eksAkt = eksisterende.firstOrNull { identifikator(it) == identifikator(spec) }
            lagEllerErstattFullmektig(fagsak, spec, eksAkt)
            spec.kontaktpersonFnr?.let {
                lagreKontaktopplysning(fagsak, spec.orgnr!!, it)
            }
        }

        // 3. Slett kontaktopplysninger for orgnr på slettede aktører som ikke gjenoppstår
        slettedeOrgnr
            .filterNot { it in nyeOrgnr }
            .forEach { slettKontaktopplysning(fagsak, it) }
    }

    /**
     * Behandler en eksisterende FULLMEKTIG-aktør som ikke matches av noen ny spec.
     * Returnerer true hvis aktøren ble slettet.
     */
    private fun håndterEksisterendeUtenMatch(
        fagsak: Fagsak,
        aktør: Aktoer,
        skjemadel: Skjemadel
    ): Boolean {
        val erVirksomhet = aktør.orgnr != null
        val erPerson = aktør.orgnr == null && aktør.personIdent != null

        return when {
            // Virksomhet + AG-del (med eller uten AT) → slett.
            // Innsendingen er autoritativ for hvem som er fullmektig-virksomhet,
            // og denne aktøren er ikke i ny spec.
            erVirksomhet && dekkerArbeidsgiverside(skjemadel) -> {
                log.info { "Sletter FULLMEKTIG-aktør ${aktør.id} (virksomhet uten match, AG-del) på sak ${fagsak.saksnummer}" }
                aktoerService.slettAktoer(aktør.id!!)
                true
            }
            // Person + AT-del → slett. AT-delen er autoritativ for person-fullmektig.
            erPerson && dekkerArbeidstakerside(skjemadel) -> {
                log.info { "Sletter FULLMEKTIG-aktør ${aktør.id} (person uten match, AT-del) på sak ${fagsak.saksnummer}" }
                aktoerService.slettAktoer(aktør.id!!)
                true
            }
            // Virksomhet + kun AT-del → fjern FULLMEKTIG_SØKNAD og tøm personIdent.
            // AG-fullmakten på denne virksomheten er ikke berørt av en AT-del.
            erVirksomhet && dekkerArbeidstakerside(skjemadel) -> {
                val nyeFullmakter = aktør.fullmaktstyper - Fullmaktstype.FULLMEKTIG_SØKNAD
                if (nyeFullmakter == aktør.fullmaktstyper) return false
                if (nyeFullmakter.isEmpty()) {
                    log.info { "Sletter FULLMEKTIG-aktør ${aktør.id} (tom etter AT-del) på sak ${fagsak.saksnummer}" }
                    aktoerService.slettAktoer(aktør.id!!)
                    return true
                }
                log.info { "Fjerner FULLMEKTIG_SØKNAD fra ${aktør.id} på sak ${fagsak.saksnummer}" }
                val dto = AktoerDto.tilDto(aktør).apply {
                    fullmakter = nyeFullmakter
                    personIdent = null
                    aktoerID = null
                }
                aktoerService.lagEllerOppdaterAktoer(fagsak, dto)
                false
            }
            // Person + AG-del (uten AT) → behold urørt. AG-delen sier ingenting om bruker-fullmakt.
            else -> false
        }
    }

    private fun lagEllerErstattFullmektig(fagsak: Fagsak, spec: FullmektigSpec, eksisterende: Aktoer?) {
        val aktørIdForPerson = spec.personIdent?.let { persondataFasade.hentAktørIdForIdent(it) }
        // Innsendingen er autoritativ — fullmaktstypene erstattes, ikke unioneres med eksisterende.
        val ønsketFullmakter = spec.fullmakter

        val erUendret = eksisterende != null &&
            eksisterende.orgnr == spec.orgnr &&
            eksisterende.personIdent == spec.personIdent &&
            eksisterende.aktørId == aktørIdForPerson &&
            eksisterende.fullmaktstyper == ønsketFullmakter
        if (erUendret) {
            log.debug { "FULLMEKTIG ${eksisterende.id} uendret, hopper over" }
            return
        }

        // Starter fra eksisterende DTO når aktøren finnes — bevarer felter som
        // institusjonsID/utenlandskPersonID som ikke skal endres her.
        val dto = (eksisterende?.let { AktoerDto.tilDto(it) } ?: AktoerDto()).apply {
            rolleKode = Aktoersroller.FULLMEKTIG.name
            orgnr = spec.orgnr
            personIdent = spec.personIdent
            aktoerID = aktørIdForPerson
            fullmakter = ønsketFullmakter
        }
        aktoerService.lagEllerOppdaterAktoer(fagsak, dto)
    }

    private fun lagreKontaktopplysning(fagsak: Fagsak, orgnr: String, kontaktpersonFnr: String) {
        val navn = persondataFasade.hentSammensattNavn(kontaktpersonFnr)
        val eksisterende = kontaktopplysningService.hentKontaktopplysning(fagsak.saksnummer, orgnr).orElse(null)
        if (eksisterende != null && eksisterende.kontaktNavn == navn && eksisterende.kontaktOrgnr == null && eksisterende.kontaktTelefon == null) {
            return
        }
        kontaktopplysningService.lagEllerOppdaterKontaktopplysning(
            fagsak.saksnummer, orgnr, null, navn, null
        )
    }

    private fun slettKontaktopplysning(fagsak: Fagsak, orgnr: String) {
        kontaktopplysningService.hentKontaktopplysning(fagsak.saksnummer, orgnr).ifPresent {
            kontaktopplysningService.slettKontaktopplysning(fagsak.saksnummer, orgnr)
        }
    }

    /**
     * Identifikator for å matche eksisterende fullmektig mot ønsket spec:
     * - For virksomhets-fullmektig: orgnr (én aktør per orgnr)
     * - For ren person-fullmektig (ingen orgnr): personIdent
     */
    private fun identifikator(aktør: Aktoer): String =
        aktør.orgnr ?: aktør.personIdent
            ?: error("FULLMEKTIG-aktør ${aktør.id} mangler både orgnr og personIdent")

    private fun identifikator(spec: FullmektigSpec): String =
        spec.orgnr ?: spec.personIdent!!  // init i FullmektigSpec krever at minst én er satt
}
