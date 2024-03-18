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
class ProsessUtil(
    private val prosessinstanserOpprettet: MutableList<Prosessinstans> = CopyOnWriteArrayList(),
    private val prosessinstanserFerdig: MutableList<Prosessinstans> = CopyOnWriteArrayList()
) {
    @EventListener
    fun prosessinstansOpprettet(prosessinstansOpprettetEvent: ProsessinstansOpprettetEvent) {
        log.info ("Prosessinstans Opprettet - ${prosessinstansOpprettetEvent.hentProsessinstans().type}")
        prosessinstanserOpprettet.add(prosessinstansOpprettetEvent.hentProsessinstans())
    }

    @EventListener
    fun prosessinstansFerdig(prosessinstansFerdigEvent: ProsessinstansFerdigEvent) {
        log.info ("Prosessinstans Ferdig - ${prosessinstansFerdigEvent.hentProsessinstans().type}")
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

    protected fun waitForProcessesToStart(count: Int) {
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

    protected fun waitForProcessesToFinnish(count: Int) {
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


    protected fun waitForAndReturnProcess(prosessType: ProsessType, startTime: LocalDateTime): Prosessinstans {
        withClue("wait for process type:$prosessType to start") {
            AwaitUtil.awaitWithFailOnLogErrors {
                pollDelay(pollDelay)
                    .timeout(timeOutFindingProsess)
                    .onTimeout { e ->
                        withClue(e.message) {
                            val types = prosessinstanserOpprettet.filter { it.registrertDato > startTime }.map { it.type }
                            types shouldContain prosessType
                        }
                    }
                    .waitUntil { prosessinstanserOpprettet.filter { it.registrertDato > startTime }.any { it.type == prosessType } }
            }
        }

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
                                prosesserStartet shouldBe ProsessStatus.FERDIG
                            }
                        }
                    }
                    .waitUntil {
                        current = prosessinstanserFerdig.filter { it.registrertDato > startTime }
                            .firstOrNull { it.type == prosessType && it.status == ProsessStatus.FERDIG }
                        current != null
                    }
                current.shouldNotBeNull()
            }
        }
    }

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
