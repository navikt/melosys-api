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
 * - AT-del oppdaterer kun FULLMEKTIG_SØKNAD; AG-del oppdaterer kun FULLMEKTIG_ARBEIDSGIVER + ARBEIDSGIVER.
 *   Komplett søknad (ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL) dekker begge sider.
 * - Match på orgnr (eller personIdent uten orgnr) og oppdater eksisterende rad — ny rad kun ved nytt orgnr/identitet.
 * - Ingen endring hvis nye opplysninger er identiske (unngår støy i Envers-historikk).
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
        val berørteFullmaktstyper = berørteFullmaktstyper(aktører.skjemadel)
        val eksisterende = aktoerService.hentfagsakAktører(fagsak, Aktoersroller.FULLMEKTIG)
        val ønskedeIder = aktører.fullmektige.map(::identifikator).toSet()

        // 1. Behandle eksisterende: fjern fullmaktstyper som er innenfor "vår side" og ikke matches av ønsket
        eksisterende.forEach { eksAkt ->
            if (identifikator(eksAkt) !in ønskedeIder) {
                fjernBerørteFullmaktstyperEllerSlett(fagsak, eksAkt, berørteFullmaktstyper)
            }
        }

        // 2. Lagre/oppdatere ønskede fullmektige + tilhørende kontaktopplysninger
        aktører.fullmektige.forEach { spec ->
            val eksAkt = eksisterende.firstOrNull { identifikator(it) == identifikator(spec) }
            lagEllerOppdaterFullmektig(fagsak, spec, eksAkt)
            spec.kontaktpersonFnr?.let {
                lagreKontaktopplysning(fagsak, spec.orgnr!!, it)
            }
        }

        // 3. Slett kontaktopplysninger for orgnr som forsvant — beregnes deterministisk fra
        //    eksisterende aktører som ble slettet (alle fullmaktstyper innenfor vår side fjernet)
        //    og som ikke gjenoppstår i nye spec-er.
        val nyeOrgnr = aktører.fullmektige.mapNotNull { it.orgnr }.toSet()
        eksisterende
            .filter { identifikator(it) !in ønskedeIder }
            .filter { (it.fullmaktstyper - berørteFullmaktstyper).isEmpty() }
            .mapNotNull { it.orgnr }
            .filterNot { it in nyeOrgnr }
            .forEach { slettKontaktopplysning(fagsak, it) }
    }

    private fun berørteFullmaktstyper(skjemadel: Skjemadel): Set<Fullmaktstype> = buildSet {
        if (dekkerArbeidstakerside(skjemadel)) add(Fullmaktstype.FULLMEKTIG_SØKNAD)
        if (dekkerArbeidsgiverside(skjemadel)) add(Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER)
    }

    private fun fjernBerørteFullmaktstyperEllerSlett(
        fagsak: Fagsak,
        aktør: Aktoer,
        berørte: Set<Fullmaktstype>
    ) {
        val nyeFullmakter = aktør.fullmaktstyper - berørte
        if (nyeFullmakter == aktør.fullmaktstyper) return

        if (nyeFullmakter.isEmpty()) {
            log.info { "Sletter FULLMEKTIG-aktør ${aktør.id} (ingen gjenværende fullmaktstyper) på sak ${fagsak.saksnummer}" }
            aktoerService.slettAktoer(aktør.id!!)
        } else {
            // Hvis FULLMEKTIG_SØKNAD fjernes fra en kombinert virksomhet+person-fullmektig,
            // skal personIdent og aktørId også fjernes — de var knyttet til søknads-fullmakten.
            val tømPerson = Fullmaktstype.FULLMEKTIG_SØKNAD in berørte && aktør.orgnr != null
            log.info { "Oppdaterer FULLMEKTIG-aktør ${aktør.id} med fullmakter $nyeFullmakter på sak ${fagsak.saksnummer}" }
            val dto = AktoerDto.tilDto(aktør).apply {
                fullmakter = nyeFullmakter
                if (tømPerson) {
                    personIdent = null
                    aktoerID = null
                }
            }
            aktoerService.lagEllerOppdaterAktoer(fagsak, dto)
        }
    }

    private fun lagEllerOppdaterFullmektig(fagsak: Fagsak, spec: FullmektigSpec, eksisterende: Aktoer?) {
        val aktørIdForPerson = spec.personIdent?.let { persondataFasade.hentAktørIdForIdent(it) }
        val ønsketFullmakter = if (eksisterende != null) {
            // Bevar fullmaktstyper på "den andre siden" som ikke er berørt av denne synkroniseringen
            eksisterende.fullmaktstyper + spec.fullmakter
        } else spec.fullmakter

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
        aktør.orgnr ?: aktør.personIdent ?: ""

    private fun identifikator(spec: FullmektigSpec): String =
        spec.orgnr ?: spec.personIdent ?: ""
}
