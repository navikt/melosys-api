package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.SkjemaSakMapping
import no.nav.melosys.domain.kodeverk.Saksstatuser
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
    private val gyldigeSaksstatuser = setOf(Saksstatuser.OPPRETTET, Saksstatuser.LOVVALG_AVKLART)

    /**
     * Finner saksnummer for relaterte skjemaId-er, men kun hvis saken har gyldig status
     * (OPPRETTET eller LOVVALG_AVKLART).
     */
    @Transactional(readOnly = true)
    fun finnSaksnummerForGyldigSak(skjemaIder: Collection<UUID>): String? {
        if (skjemaIder.isEmpty()) return null

        val mappinger = skjemaSakMappingRepository.findBySkjemaIdIn(skjemaIder)
        if (mappinger.isEmpty()) return null

        // Finn saksnumre og sjekk status
        val saksnumre = mappinger.map { it.saksnummer }.distinct()
        for (saksnummer in saksnumre) {
            val fagsak = fagsakRepository.findBySaksnummer(saksnummer).orElse(null) ?: continue
            if (fagsak.status in gyldigeSaksstatuser) {
                log.info { "Fant gyldig sak $saksnummer (status=${fagsak.status}) for skjemaIder" }
                return saksnummer
            }
        }

        log.info { "Fant ${mappinger.size} mappinger men ingen sak med gyldig status" }
        return null
    }

    @Transactional
    fun lagreMapping(
        skjemaId: UUID,
        saksnummer: String,
        mottatteOpplysningerId: Long? = null,
        originalData: String? = null,
        innsendtDato: Instant? = null
    ) {
        if (skjemaSakMappingRepository.findBySkjemaId(skjemaId).isPresent) {
            log.debug { "Mapping for skjemaId=$skjemaId eksisterer allerede" }
            return
        }

        skjemaSakMappingRepository.save(
            SkjemaSakMapping(
                skjemaId = skjemaId,
                saksnummer = saksnummer,
                mottatteOpplysningerId = mottatteOpplysningerId,
                originalData = originalData,
                innsendtDato = innsendtDato
            )
        )
        log.info { "Lagret mapping: skjemaId=$skjemaId → saksnummer=$saksnummer" }
    }

    @Transactional
    fun lagreMappinger(
        skjemaIder: Collection<UUID>,
        saksnummer: String,
        mottatteOpplysningerId: Long? = null,
        originalData: String? = null,
        innsendtDato: Instant? = null
    ) {
        skjemaIder.forEach { skjemaId ->
            lagreMapping(skjemaId, saksnummer, mottatteOpplysningerId, originalData, innsendtDato)
        }
    }

    @Transactional
    fun oppdaterJournalpostId(skjemaId: UUID, journalpostId: String) {
        val mapping = skjemaSakMappingRepository.findBySkjemaId(skjemaId).orElse(null)
        if (mapping != null) {
            mapping.journalpostId = journalpostId
            skjemaSakMappingRepository.save(mapping)
            log.info { "Oppdatert journalpostId=$journalpostId for skjemaId=$skjemaId" }
        }
    }
}
