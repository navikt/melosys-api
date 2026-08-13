package no.nav.melosys.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.PersistenceException
import mu.KotlinLogging
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/**
 * Cross-instance lås for atomisk sak-resolusjon ved mottak av digital søknad (MELOSYS-8151).
 *
 * Brukes til å serialisere «ny vs. eksisterende sak»-avgjørelsen per person (aktørId), slik at
 * relaterte deler av samme søknad som konsumeres samtidig ikke oppretter duplikate fagsaker.
 *
 * Protokollen er to steg:
 *  1. [sikreLåsRad] — idempotent opprettelse av lås-raden i en EGEN transaksjon, slik at en
 *     sjelden samtidig-insert-kollisjon (ORA-00001) ikke forurenser kallerens transaksjon.
 *  2. [taRadlås] — `SELECT ... FOR UPDATE` på raden i KALLERENS transaksjon (PROPAGATION_MANDATORY).
 *     Den eksklusive radlåsen holdes til kallerens transaksjon committer/rulles tilbake.
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
     * MERGE er idempotent; ved et samtidig førstegangs-innsett kan Oracle kaste unik-feil — da
     * finnes raden allerede (den andre transaksjonen vant), og vi kan trygt fortsette.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun sikreLåsRad(aktørId: String) {
        try {
            entityManager.createNativeQuery(
                """
                MERGE INTO digital_soknad_sak_lock l
                USING (SELECT :aktoerId AS aktoer_id FROM dual) s
                ON (l.aktoer_id = s.aktoer_id)
                WHEN NOT MATCHED THEN INSERT (aktoer_id) VALUES (s.aktoer_id)
                """.trimIndent()
            ).setParameter("aktoerId", aktørId).executeUpdate()
        } catch (e: PersistenceException) {
            log.info { "Lås-rad for aktørId fantes allerede (samtidig opprettelse vant kappløpet): ${e.message}" }
        }
    }

    /**
     * Tar en eksklusiv radlås på [aktørId] i kallerens transaksjon. Blokkerer til en eventuell
     * konkurrerende transaksjon på samme aktørId committer. Holdes til kallerens transaksjon
     * committer. MÅ kalles innenfor en aktiv transaksjon (PROPAGATION_MANDATORY), ellers ville
     * låsen slippes umiddelbart og være verdiløs. [sikreLåsRad] må ha kjørt først.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun taRadlås(aktørId: String) {
        entityManager.createNativeQuery(
            "SELECT aktoer_id FROM digital_soknad_sak_lock WHERE aktoer_id = :aktoerId FOR UPDATE"
        ).setParameter("aktoerId", aktørId).singleResult
    }
}
