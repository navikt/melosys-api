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

    /**
     * Frikobler skjema_sak_mapping-rader fra en MottatteOpplysninger som skal slettes, slik at
     * FK_SKJEMA_SAK_MAPPING_MOTTOPP ikke blokkerer slettingen (ORA-02292).
     *
     * Endringen flushes umiddelbart slik at FK-en er nullstilt i databasen før mottatteopplysninger
     * slettes. Returnerer skjemaId-ene som ble frikoblet, slik at de kan re-pekes til ny
     * MottatteOpplysninger ved gjenoppretting.
     */
    @Transactional
    fun frikobleFraMottatteOpplysninger(mottatteOpplysninger: MottatteOpplysninger): List<UUID> {
        val mappinger = skjemaSakMappingRepository.findByMottatteOpplysninger_Id(mottatteOpplysninger.id)
        if (mappinger.isEmpty()) return emptyList()

        mappinger.forEach { it.mottatteOpplysninger = null }
        skjemaSakMappingRepository.saveAllAndFlush(mappinger)
        log.info { "Frikoblet ${mappinger.size} skjema-sak-mapping(er) fra mottatteOpplysninger=${mottatteOpplysninger.id}" }
        return mappinger.map { it.skjemaId }
    }

    /**
     * Re-peker skjema_sak_mapping-rader til en ny MottatteOpplysninger etter gjenoppretting,
     * slik at koblingen skjema → sak/mottatte opplysninger bevares.
     */
    @Transactional
    fun knyttTilMottatteOpplysninger(skjemaIder: Collection<UUID>, mottatteOpplysninger: MottatteOpplysninger) {
        if (skjemaIder.isEmpty()) return

        val mappinger = skjemaSakMappingRepository.findBySkjemaIdIn(skjemaIder)
        mappinger.forEach { it.mottatteOpplysninger = mottatteOpplysninger }
        skjemaSakMappingRepository.saveAll(mappinger)
        log.info { "Re-pekte ${mappinger.size} skjema-sak-mapping(er) til mottatteOpplysninger=${mottatteOpplysninger.id}" }
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
