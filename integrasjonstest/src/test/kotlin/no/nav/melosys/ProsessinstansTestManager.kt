package no.nav.melosys

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import no.nav.melosys.AwaitUtil.onTimeout
import no.nav.melosys.AwaitUtil.waitUntil
import no.nav.melosys.saksflyt.ProsessinstansFerdigEvent
import no.nav.melosys.saksflytapi.ProsessinstansOpprettetEvent
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

private val log = KotlinLogging.logger { }

@Component
class ProsessinstansTestManager(
    private val prosessinstanserOpprettet: MutableList<Prosessinstans> = CopyOnWriteArrayList(),
    private val prosessinstanserFerdig: MutableList<Prosessinstans> = CopyOnWriteArrayList()
) {
    @EventListener
    private fun prosessinstansOpprettet(prosessinstansOpprettetEvent: ProsessinstansOpprettetEvent) {
        log.info("Prosessinstans ${prosessinstansOpprettetEvent.hentProsessinstans().type} - Opprettet")
        prosessinstanserOpprettet.add(prosessinstansOpprettetEvent.hentProsessinstans())
    }

    @EventListener
    private fun prosessinstansFerdig(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        log.info("Prosessinstans ${prosessinstansFerdigEvent.hentProsessinstans().type} - Ferdig")
        prosessinstanserFerdig.add(prosessinstansFerdigEvent.hentProsessinstans())
    }

    val prosessinstanserOpprettetCount: Int
        get() = prosessinstanserOpprettet.size

    fun clear() {
        prosessinstanserOpprettet.clear()
        prosessinstanserFerdig.clear()
    }

    fun executeAndWait(
        waitForProsesses: Map<ProsessType, Int>,
        returnProsessOfType: ProsessType = waitForProsesses.keys.first(),
        onWaitUntil: () -> Unit = {},
        process: () -> Unit
    ): Prosessinstans {
        try {
            process()
            waitForProcessesToStart(waitForProsesses)
            log.info { "prosessinstanser started ${waitForProsesses.keys}" }
            return withClue("Wait for ${waitForProsesses.keys}") {
                waitForAllProcessesToFinish(waitForProsesses, onWaitUntil)
                prosessinstanserFerdig
                    .firstOrNull { it.type == returnProsessOfType }
                    ?: error("Fant ikke prosess for $returnProsessOfType")
            }.also {
                // Vi sjekker dette igjen siden vi kan få false positive første gang vi sjekker
                // Det kan skje at kun en prosess er startet, og waitForProsesses inneholder feilaktig bare denne
                prosessinstanserOpprettet.toTypeToCountMap() shouldBe waitForProsesses
            }
        } finally {
            clear()
        }
    }

    private fun waitForProcessesToStart(waitForProsesses: Map<ProsessType, Int>) {
        withClue("wait for $waitForProsesses processes to start") {
            AwaitUtil.awaitWithFailOnLogErrors {
                pollDelay(pollDelay)
                    .timeout(timeOutFindingProsess)
                    .onTimeout { e ->
                        val abortMessage =
                            if (e is AwaitUtil.ConditionAbortException) "waitUntil was aborted because because the number of created process instances (${
                                prosessinstanserOpprettet.size
                            }) >  exceeds the expected total (${waitForProsesses.values.sum()})"
                            else "waitUntil timed out"
                        withClue(abortMessage) {
                            prosessinstanserOpprettet.toTypeToCountMap() shouldBe waitForProsesses
                        }
                    }.waitUntil(abort = { prosessinstanserOpprettet.size > waitForProsesses.values.sum() }) {
                        waitForProsesses == prosessinstanserOpprettet.toTypeToCountMap()
                    }
            }
        }
    }

    /**
     * Venter til ALLE forventede prosesser har status FERDIG (ikke bare én per type).
     * Forhindrer at asynkrone prosesser fra forrige test fortsetter å kjøre
     * etter at truncateAllTables() har ryddet databasen.
     */
    private fun waitForAllProcessesToFinish(
        waitForProsesses: Map<ProsessType, Int>,
        onWaitUntil: () -> Unit
    ) {
        val expectedTotal = waitForProsesses.values.sum()
        withClue("wait for all $expectedTotal processes to finish") {
            AwaitUtil.awaitWithFailOnLogErrors {
                pollDelay(pollDelay)
                    .timeout(timeOut)
                    .onTimeout { e ->
                        val ferdigByType = prosessinstanserFerdig
                            .filter { it.status == ProsessStatus.FERDIG }
                            .toTypeToCountMap()
                        withClue("${ferdigByType.values.sum()}/$expectedTotal finished. " +
                                "Ferdig: $ferdigByType, Forventet: $waitForProsesses") {
                            ferdigByType shouldBe waitForProsesses
                        }
                    }
                    .waitUntil(abort = { prosessinstanserOpprettet.any { it.status == ProsessStatus.FEILET } }) {
                        onWaitUntil()
                        prosessinstanserFerdig.count { it.status == ProsessStatus.FERDIG } >= expectedTotal
                    }
            }
        }
        log.info { "Alle $expectedTotal prosesser ferdig: ${prosessinstanserFerdig.toTypeToCountMap()}" }
    }

    private fun List<Prosessinstans>.toTypeToCountMap(): Map<ProsessType, Int> = groupBy { it.type }.mapValues { it.value.size }

    companion object {
        private val defaultTimeOut: Duration = Duration.ofSeconds(30)
        private val defaultTimeOutFindingProsess: Duration = Duration.ofSeconds(5)
        private val defaultPollInterval: Duration = Duration.ofMillis(200)
        private val defaultPollDelay: Duration = Duration.ofMillis(100)

        internal var timeOut: Duration = defaultTimeOut
        internal var timeOutFindingProsess: Duration = defaultTimeOutFindingProsess
        internal var pollInterval: Duration = defaultPollInterval
        internal var pollDelay: Duration = defaultPollDelay

        fun reset() {
            timeOut = defaultTimeOut
            timeOutFindingProsess = defaultTimeOutFindingProsess
            pollInterval = defaultPollInterval
            pollDelay = defaultPollDelay
        }
    }
}
