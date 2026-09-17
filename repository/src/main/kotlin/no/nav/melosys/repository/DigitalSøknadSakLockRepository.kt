package no.nav.melosys.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.NoResultException
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/** Hvor lenge vi venter på en opptatt lås før vi gir opp, i sekunder. */
private const val LÅS_TIMEOUT_SEKUNDER = 10

/**
 * Cross-instance lås for atomisk sak-resolusjon ved mottak av digital søknad (MELOSYS-8151).
 *
 * Brukes til å serialisere «ny vs. eksisterende sak»-avgjørelsen per person (aktørId), slik at
 * relaterte deler av samme søknad som konsumeres samtidig ikke oppretter duplikate fagsaker.
 *
 * Protokollen er to steg:
 *  1. [sikreLåsRad] — idempotent opprettelse av lås-raden i en EGEN transaksjon, slik at en
 *     sjelden samtidig-insert-kollisjon (ORA-00001) ikke forurenser kallerens transaksjon.
 *  2. [taRadlås] — `SELECT ... FOR UPDATE WAIT` på raden i KALLERENS transaksjon
 *     (PROPAGATION_MANDATORY). Den eksklusive radlåsen holdes til kallerens transaksjon
 *     committer/rulles tilbake.
 *
 * Begge må kalles som separate metodekall på denne bønnen (ikke via en intern hjelpemetode),
 * ellers omgås Spring-proxyen og transaksjonssemantikken.
 */
@Repository
class DigitalSøknadSakLockRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /**
     * Sørger for at lås-raden for [aktørId] finnes. Kjører i egen transaksjon (REQUIRES_NEW) som
     * committes umiddelbart, slik at raden er synlig for andre transaksjoner før radlåsen tas.
     *
     * MERGE er idempotent, men ved et samtidig førstegangs-innsett kan Oracle kaste unik-feil.
     * Den feilen håndteres av kalleren ([sikreLåsRadOgTaRadlås]) og IKKE her inne: en exception
     * etter en constraint-violation gjør Hibernate-sesjonen ubrukelig, og et catch på innsiden av
     * den transaksjonelle metoden ville uansett bare flyttet feilen til commit-tidspunktet.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun sikreLåsRad(aktørId: String) {
        entityManager.createNativeQuery(
            """
            MERGE INTO digital_soknad_sak_lock l
            USING (SELECT :aktoerId AS aktoer_id FROM dual) s
            ON (l.aktoer_id = s.aktoer_id)
            WHEN NOT MATCHED THEN INSERT (aktoer_id) VALUES (s.aktoer_id)
            """.trimIndent()
        ).setParameter("aktoerId", aktørId).executeUpdate()
    }

    /**
     * Tar en eksklusiv radlås på [aktørId] i kallerens transaksjon. Blokkerer til en eventuell
     * konkurrerende transaksjon på samme aktørId committer. Holdes til kallerens transaksjon
     * committer. MÅ kalles innenfor en aktiv transaksjon (PROPAGATION_MANDATORY), ellers ville
     * låsen slippes umiddelbart og være verdiløs. [sikreLåsRad] må ha kjørt først.
     *
     * `WAIT` gjør at vi gir opp etter [LÅS_TIMEOUT_SEKUNDER] i stedet for å blokkere i det
     * uendelige. En hengende transaksjon skal ikke kunne holde en saksflyt-tråd for alltid; da er
     * det bedre at steget feiler og sagaen restartes.
     *
     * @throws org.springframework.dao.PessimisticLockingFailureException hvis låsen ikke er ledig
     *   innen tidsfristen (Oracle ORA-30006), oversatt av Springs exception translation.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun taRadlås(aktørId: String) {
        try {
            entityManager.createNativeQuery(
                "SELECT aktoer_id FROM digital_soknad_sak_lock WHERE aktoer_id = :aktoerId " +
                    "FOR UPDATE WAIT $LÅS_TIMEOUT_SEKUNDER"
            ).setParameter("aktoerId", aktørId).singleResult
        } catch (e: NoResultException) {
            // Raden skal være opprettet av sikreLåsRad. Skjer den ikke finnes, er noe galt med
            // rekkefølgen — feil høylytt i stedet for å fortsette uten lås og risikere duplikatsak.
            throw IllegalStateException("Lås-rad for aktørId mangler — sikreLåsRad må kalles først", e)
        }
    }

}

/**
 * Tar digital-søknad-låsen i to steg mot [DigitalSøknadSakLockRepository].
 *
 * Egen bønne fordi de to stegene må gå gjennom Spring-proxyen for å få hver sin
 * transaksjonssemantikk (REQUIRES_NEW for radopprettelsen, MANDATORY for radlåsen). Et internt
 * selvkall i repositoriet ville omgått proxyen, og radopprettelsen ville havnet i kallerens
 * transaksjon — der en unik-feil ikke lar seg fange.
 */
@Component
class DigitalSøknadSakLås(
    private val lockRepository: DigitalSøknadSakLockRepository
) {

    /**
     * Låser [aktørId] for resten av kallerens transaksjon.
     *
     * Håndterer at to instanser oppretter lås-raden samtidig: da feiler radopprettelsen med
     * unik-feil i sin egen transaksjon (som rulles rent tilbake), raden finnes uansett, og vi går
     * videre til å ta radlåsen.
     */
    fun lås(aktørId: String) {
        try {
            lockRepository.sikreLåsRad(aktørId)
        } catch (e: Exception) {
            log.info { "Kunne ikke opprette lås-rad for aktørId, antar samtidig opprettelse: ${e.message}" }
        }
        lockRepository.taRadlås(aktørId)
    }
}
