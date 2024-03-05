package no.nav.melosys

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.AwaitUtil.builder
import no.nav.melosys.AwaitUtil.untilMatching
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@Component
class ProsessUtil(
    private val prosessinstansRepository: ProsessinstansRepository
) {
    fun executeAndWait(
        waitForprosessType: ProsessType,
        alsoWaitForprosessType: List<ProsessType> = listOf(),
        process: () -> Unit
    ): Prosessinstans {
        val startTime = LocalDateTime.now()
        process()
        val journalføringProsessID = finnProsess(waitForprosessType, startTime)
        withClue("also wait for prosessTypes: $alsoWaitForprosessType") {
            alsoWaitForprosessType.forEach { finnProsess(it, startTime) }
        }
        return prosessinstansRepository.findById(journalføringProsessID).get()
    }

    protected fun finnProsess(prosessType: ProsessType, startTid: LocalDateTime): UUID {
        withClue("wait for prosees type:$prosessType to start") {
            AwaitUtil.awaitWithFailOnLogErrors {
                pollDelay(pollDelay)
                    .timeout(timeOutFindingProsess)
                    .builder<Unit>()
                    .waitFor { prosessinstansRepository.findAllAfterDate(startTid).any { it.type == prosessType } }
                    .assertIfTimeout { e ->
                        val types = prosessinstansRepository.findAllAfterDate(startTid).map { it.type }
                        withClue(e.message) {
                            types shouldContain prosessType
                        }
                    }
                    .execute()
            }
        }

        withClue("wait for prosees type:$prosessType to have status FERDIG") {
            return AwaitUtil.awaitWithFailOnLogErrors {
                var current: Prosessinstans? = null
                timeout(timeOut)
                    .pollInterval(pollInterval)
                    .untilMatching(waitFor = prosessType) {
                        current = prosessinstansRepository.findAllAfterDate(startTid)
                            .firstOrNull { it.type == prosessType && it.status == ProsessStatus.FERDIG }
                        current?.type
                    }
                current.shouldNotBeNull().id
            }
        }
    }

    companion object {
        private val defaultTimeOut: Duration = Duration.ofSeconds(30)
        private val defaultTimeOutFindingProsess: Duration = Duration.ofSeconds(5)
        private val defaultPollInterval: Duration = Duration.ofSeconds(1)
        private val defaultPollDelay: Duration = Duration.ofSeconds(1)

        internal var timeOut: Duration = Duration.ofSeconds(30)
        internal var timeOutFindingProsess: Duration = Duration.ofSeconds(5)
        internal var pollInterval: Duration = Duration.ofSeconds(1)
        internal var pollDelay: Duration = Duration.ofSeconds(1)

        fun reset() {
            timeOut = defaultTimeOut
            timeOutFindingProsess = defaultTimeOutFindingProsess
            pollInterval = defaultPollInterval
            pollDelay = defaultPollDelay
        }
    }

}
