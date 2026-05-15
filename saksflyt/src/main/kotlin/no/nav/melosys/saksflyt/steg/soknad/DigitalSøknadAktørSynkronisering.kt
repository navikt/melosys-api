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
 * - Match på orgnr → eksisterende aktør oppdateres med spec.fullmakter.
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

    private fun synkroniserArbeidsgivere(fagsak: Fagsak, ønskedeOrgnumre: List<String>) {
        val eksisterende = aktoerService.hentfagsakAktører(fagsak, Aktoersroller.ARBEIDSGIVER)
            .mapNotNull { it.orgnr }.toSet()
        if (eksisterende == ønskedeOrgnumre.toSet()) {
            log.debug { "Arbeidsgiver-aktører uendret på sak ${fagsak.saksnummer}, hopper over" }
            return
        }
        aktoerService.erstattEksisterendeArbeidsgiveraktører(fagsak, ønskedeOrgnumre)
    }

    private fun synkroniserFullmektige(fagsak: Fagsak, aktørerFraSøknad: AktørerFraSøknad) {
        val eksisterendeAktører = aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG)
        val ønskedeIdentifikatorer = aktørerFraSøknad.fullmektige.map(::identifikator).toSet()
        val nyeOrgnr = aktørerFraSøknad.fullmektige.mapNotNull { it.orgnr }.toSet()

        // 1. Eksisterende aktører som ikke finnes i ny innsending — slett eller oppdater
        val slettedeOrgnr = mutableSetOf<String>()
        eksisterendeAktører.forEach { eksisterendeAktør ->
            if (identifikator(eksisterendeAktør) !in ønskedeIdentifikatorer) {
                val bleSlettet = fjernEllerOppdaterEksisterendeAktør(fagsak, eksisterendeAktør, aktørerFraSøknad.skjemadel)
                if (bleSlettet) eksisterendeAktør.orgnr?.let { slettedeOrgnr.add(it) }
            }
        }

        // 2. Lagre/oppdatere ønskede fullmektige + tilhørende kontaktopplysninger
        aktørerFraSøknad.fullmektige.forEach { fullmektigSpec ->
            val eksisterendeAktør = eksisterendeAktører.firstOrNull { identifikator(it) == identifikator(fullmektigSpec) }
            lagEllerErstattFullmektig(fagsak, fullmektigSpec, eksisterendeAktør)
            fullmektigSpec.kontaktpersonFnr?.let {
                lagreKontaktopplysning(fagsak, requireNotNull(fullmektigSpec.orgnr), it)
            }
        }

        // 3. Slett kontaktopplysninger for orgnr på slettede aktører som ikke gjenoppstår
        slettedeOrgnr
            .filterNot { it in nyeOrgnr }
            .forEach { slettKontaktopplysning(fagsak, it) }
    }

    /**
     * Behandler en eksisterende FULLMEKTIG-aktør som ikke finnes i ny innsending.
     * Returnerer true hvis aktøren ble slettet.
     */
    private fun fjernEllerOppdaterEksisterendeAktør(
        fagsak: Fagsak,
        aktør: Aktoer,
        skjemadel: Skjemadel
    ): Boolean {
        val erVirksomhet = aktør.orgnr != null
        val erPerson = aktør.orgnr == null && aktør.personIdent != null

        return when {
            // Innsendingen er autoritativ for hvem som er fullmektig-virksomhet,
            // og denne aktøren finnes ikke i innsendingen.
            erVirksomhet && dekkerArbeidsgiverside(skjemadel) -> {
                log.info { "Sletter FULLMEKTIG-aktør ${aktør.id} (virksomhet ikke i innsending, AG-del) på sak ${fagsak.saksnummer}" }
                aktoerService.slettAktoer(requireNotNull(aktør.id))
                true
            }
            // AT-delen er autoritativ for person-fullmektig, og denne aktøren finnes ikke i innsendingen.
            erPerson && dekkerArbeidstakerside(skjemadel) -> {
                log.info { "Sletter FULLMEKTIG-aktør ${aktør.id} (person ikke i innsending, AT-del) på sak ${fagsak.saksnummer}" }
                aktoerService.slettAktoer(requireNotNull(aktør.id))
                true
            }
            // AG-fullmakten på denne virksomheten er ikke berørt av en AT-del,
            // så vi fjerner kun FULLMEKTIG_SØKNAD og tømmer personIdent.
            erVirksomhet && dekkerArbeidstakerside(skjemadel) -> {
                val nyeFullmakter = aktør.fullmaktstyper - Fullmaktstype.FULLMEKTIG_SØKNAD
                if (nyeFullmakter == aktør.fullmaktstyper) return false
                if (nyeFullmakter.isEmpty()) {
                    log.info { "Sletter FULLMEKTIG-aktør ${aktør.id} (tom etter AT-del) på sak ${fagsak.saksnummer}" }
                    aktoerService.slettAktoer(requireNotNull(aktør.id))
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
            // AG-del sier ingenting om bruker-fullmakt; person-aktøren forblir urørt.
            else -> false
        }
    }

    private fun lagEllerErstattFullmektig(fagsak: Fagsak, spec: FullmektigSpec, eksisterendeAktør: Aktoer?) {
        val aktørIdForPerson = spec.personIdent?.let { persondataFasade.hentAktørIdForIdent(it) }
        val ønsketFullmakter = spec.fullmakter

        val erUendret = eksisterendeAktør != null &&
            eksisterendeAktør.orgnr == spec.orgnr &&
            eksisterendeAktør.personIdent == spec.personIdent &&
            eksisterendeAktør.aktørId == aktørIdForPerson &&
            eksisterendeAktør.fullmaktstyper == ønsketFullmakter
        if (erUendret) {
            log.debug { "FULLMEKTIG ${eksisterendeAktør.id} uendret, hopper over" }
            return
        }

        // Bevarer felter som ikke styres herfra (institusjonsID, utenlandskPersonID).
        val dto = (eksisterendeAktør?.let { AktoerDto.tilDto(it) } ?: AktoerDto()).apply {
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
        // Samme navn → behold raden uendret (slik at manuelt registrert telefon/orgnr ikke nullstilles).
        if (eksisterende != null && eksisterende.kontaktNavn == navn) {
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
        spec.orgnr ?: requireNotNull(spec.personIdent) {
            "FullmektigSpec uten orgnr må ha personIdent"
        }
}
