package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.SkjemaSakMapping
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.repository.SkjemaSakMappingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger { }

@Service
class SkjemaSakMappingService(
    private val skjemaSakMappingRepository: SkjemaSakMappingRepository,
    private val fagsakRepository: FagsakRepository
) {
    //TODO: Er det faktisk mulig å opprette ny vurdering med statusene lovvalg_avklart?
    // Og er det flere andre statuser der man kan lage ny vurdering utover lovvalg_avklart?
    private val gyldigeSaksstatuser = setOf(Saksstatuser.OPPRETTET, Saksstatuser.LOVVALG_AVKLART)

    /**
     * Finner saksnummer for relaterte skjemaId-er, men kun hvis saken har gyldig status
     * (OPPRETTET eller LOVVALG_AVKLART).
     *
     * Kaster exception hvis flere åpne saker finnes — det indikerer datainkonsistens.
     */
    @Transactional(readOnly = true)
    fun finnGyldigSaksnummerForSkjemaIder(skjemaIder: Collection<UUID>): String? {
        if (skjemaIder.isEmpty()) return null

        val mappinger = skjemaSakMappingRepository.findBySkjemaIdIn(skjemaIder)
        if (mappinger.isEmpty()) return null

        val saksnumre = mappinger.map { it.saksnummer }.distinct()
        val gyldige = fagsakRepository.findAllBySaksnummerIn(saksnumre)
            .filter { it.status in gyldigeSaksstatuser }
            .map { it.saksnummer }

        return when {
            gyldige.isEmpty() -> {
                log.info { "Fant ${mappinger.size} mappinger men ingen sak med gyldig status" }
                null
            }

            gyldige.size == 1 -> {
                log.info { "Fant gyldig sak ${gyldige.first()} for skjemaIder" }
                gyldige.first()
            }

            else -> throw IllegalStateException(
                "Fant ${gyldige.size} åpne saker (${gyldige.joinToString()}) for relaterte skjemaIder — forventet maks 1"
            )
        }
    }

    @Transactional
    fun lagreMapping(
        skjemaId: UUID,
        fagsak: Fagsak,
        mottatteOpplysninger: MottatteOpplysninger,
        originalData: String,
        innsendtDato: Instant
    ) {
        skjemaSakMappingRepository.save(
            SkjemaSakMapping(
                skjemaId = skjemaId,
                fagsak = fagsak,
                mottatteOpplysninger = mottatteOpplysninger,
                originalData = originalData,
                innsendtDato = innsendtDato
            )
        )
        log.info { "Lagret mapping: skjemaId=$skjemaId → saksnummer=${fagsak.saksnummer}" }
    }

    @Transactional
    fun oppdaterJournalpostId(skjemaId: UUID, journalpostId: String) {
        val mapping = skjemaSakMappingRepository.findBySkjemaId(skjemaId).orElse(null)
        if (mapping != null) {
            mapping.journalpostId = journalpostId
            skjemaSakMappingRepository.save(mapping)
            log.info { "Oppdatert journalpostId=$journalpostId for skjemaId=$skjemaId" }
        } else {
            log.warn { "Kan ikke oppdatere journalpostId — ingen mapping funnet for skjemaId=$skjemaId" }
        }
    }
}
