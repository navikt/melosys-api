package no.nav.melosys

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
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
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

private val log = KotlinLogging.logger { }

@Component
class ProsessinstansTestManager(
    private val prosessinstanserOpprettet: MutableList<Prosessinstans> = CopyOnWriteArrayList(),
    private val prosessinstanserFerdig: MutableList<Prosessinstans> = CopyOnWriteArrayList()
) {
    @EventListener
    private fun prosessinstansOpprettet(prosessinstansOpprettetEvent: ProsessinstansOpprettetEvent) {
        log.info("Prosessinstans Opprettet - ${prosessinstansOpprettetEvent.hentProsessinstans().type}")
        prosessinstanserOpprettet.add(prosessinstansOpprettetEvent.hentProsessinstans())
    }

    @EventListener
    private fun prosessinstansFerdig(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        log.info("Prosessinstans Ferdig - ${prosessinstansFerdigEvent.hentProsessinstans().type}")
        prosessinstanserFerdig.add(prosessinstansFerdigEvent.hentProsessinstans())
    }

    fun clear() {
        prosessinstanserOpprettet.clear()
        prosessinstanserFerdig.clear()
    }

    fun executeAndWait(
        waitForprosessType: ProsessType,
        alsoWaitForprosessType: List<ProsessType> = listOf(),
        waitForProcessCount: Int = 0,
        process: () -> Unit
    ): Prosessinstans {
        val startTime = LocalDateTime.now()
        process()
        if (waitForProcessCount > 0) waitForProcessesToStart(waitForProcessCount)
        val journalføringProsess = waitForAndReturnProcess(waitForprosessType, startTime)
        withClue("also wait for prosessTypes: $alsoWaitForprosessType") {
            alsoWaitForprosessType.forEach { waitForAndReturnProcess(it, startTime) }
        }
        withClue("started prosess types should be in waitForprosessType or alsoWaitForprosessType") {
            val waitFor = alsoWaitForprosessType + waitForprosessType
            prosessinstanserOpprettet.filter { it.registrertDato > startTime }.forEach {
                waitFor shouldContain it.type
            }
        }
        if (waitForProcessCount > 0) waitForProcessesToFinnish(waitForProcessCount)
        return journalføringProsess
    }

    fun executeAndWait(
        waitForProsesses: Map<ProsessType, Int>,
        returnProsessOfType: ProsessType = waitForProsesses.keys.first(),
        process: () -> Unit
    ): Prosessinstans {
        val startTime = LocalDateTime.now()
        process()
        waitForProcessesToStart(waitForProsesses, startTime)
        val prosessTypes = waitForProsesses.map { it.key }
        return withClue("Wait for $prosessTypes") {
            prosessTypes
                .associateWith { waitForAndReturnProcess(it, startTime) }
                .entries.groupBy({ it.key }, { it.value })
                .get(returnProsessOfType)?.firstOrNull() ?: error("Fant ikke prosess for $returnProsessOfType")
        }
    }

    private fun waitForProcessesToStart(waitForProsesses: Map<ProsessType, Int>, startTime: LocalDateTime) {
        withClue("wait for $waitForProsesses processes to start") {
            AwaitUtil.awaitWithFailOnLogErrors {
                pollDelay(pollDelay)
                    .timeout(timeOutFindingProsess)
                    .onTimeout { e ->
                        val abortMessage =
                            if (e is AwaitUtil.ConditionAbortException) "waitUntil was aborted because because the number of created process instances (${
                                prosessinstanserOpprettet.filterOnTime(startTime).size
                            }) >  exceeds the expected total (${waitForProsesses.values.sum()})"
                            else "waitUntil timed out"
                        withClue(abortMessage) {
                            prosessinstanserOpprettet.filterOnTime(startTime).toTypeToCountMap() shouldBe waitForProsesses
                        }
                    }.waitUntil(abort = { prosessinstanserOpprettet.filterOnTime(startTime).size > waitForProsesses.values.sum() }) {
                        waitForProsesses == prosessinstanserOpprettet.filterOnTime(startTime).toTypeToCountMap()
                    }
            }
        }
    }

    private fun waitForProcessesToStart(count: Int) {
        withClue("wait for $count processes to start") {
            AwaitUtil.awaitWithFailOnLogErrors {
                pollDelay(pollDelay)
                    .timeout(timeOutFindingProsess)
                    .onTimeout { e ->
                        withClue("wait for $count processes to start - ${e.message}") {
                            prosessinstanserOpprettet.shouldHaveSize(count)
                        }
                    }.waitUntil { prosessinstanserOpprettet.size == count }
            }
        }
    }

    private fun waitForProcessesToFinnish(count: Int) {
        withClue("wait for $count processes to finnish") {
            AwaitUtil.awaitWithFailOnLogErrors {
                pollDelay(pollDelay)
                    .timeout(timeOutFindingProsess)
                    .onTimeout { e ->
                        withClue("wait for $count processes to finnish - ${e.message}") {
                            prosessinstanserFerdig.shouldHaveSize(count)
                        }
                    }.waitUntil { prosessinstanserFerdig.size == count }
            }
        }
    }

    private fun waitForAndReturnProcess(prosessType: ProsessType, startTime: LocalDateTime): Prosessinstans {
        withClue("wait for prosees type:$prosessType to have status FERDIG") {
            return AwaitUtil.awaitWithFailOnLogErrors {
                var current: Prosessinstans? = null
                pollDelay(pollDelay)
                    .timeout(timeOut)
                    .onTimeout { e ->
                        val prosesserStartet = prosessinstanserFerdig.filter { it.registrertDato > startTime }
                            .firstOrNull { it.type == prosessType }?.status
                        withClue(e.message) {
                            withClue("prosess med type: $prosessType har status $prosesserStartet") {
                                prosessinstanserOpprettet.filter { it.registrertDato > startTime }.map { it.type } shouldContain prosessType
                                prosessinstanserOpprettet.filter { it.registrertDato > startTime }.firstOrNull() { it.type == prosessType }
                                    ?.status shouldBe ProsessStatus.FERDIG
                            }
                        }
                    }
                    .waitUntil(abort = { prosessinstanserOpprettet.any { it.status == ProsessStatus.FEILET } }) {
                        current = prosessinstanserFerdig.filter { it.registrertDato > startTime }
                            .firstOrNull { it.type == prosessType && it.status == ProsessStatus.FERDIG }
                        current != null
                    }
                current.shouldNotBeNull()
            }
        }
    }


    private fun List<Prosessinstans>.toTypeToCountMap(): Map<ProsessType, Int> {
        return groupBy { it.type }.mapValues { it.value.size }
    }

    private fun List<Prosessinstans>.filterOnTime(startTime: LocalDateTime) = filter { it.registrertDato > startTime }

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
