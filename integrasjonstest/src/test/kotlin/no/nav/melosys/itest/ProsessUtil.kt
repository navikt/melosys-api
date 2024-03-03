package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldContain
import no.nav.melosys.AwaitUtil
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.awaitility.kotlin.untilNotNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

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
        alsoWaitForprosessType.forEach { finnProsess(it, startTime) }
        return prosessinstansRepository.findById(journalføringProsessID).get()
    }

    protected fun finnProsess(prosessType: ProsessType, startTid: LocalDateTime): UUID {
        AwaitUtil.awaitWithFailOnLogErrors {
            pollDelay(1, TimeUnit.SECONDS)
                .timeout(30, TimeUnit.SECONDS)
                .untilNotNull {
                    prosessinstansRepository.findAllAfterDate(startTid)
                }.map { it.type }.shouldContain(prosessType)
        }

        return AwaitUtil.awaitWithFailOnLogErrors {
            timeout(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilNotNull {
                    prosessinstansRepository.findAllAfterDate(startTid)
                        .find { it.type == prosessType && it.status == ProsessStatus.FERDIG }?.id
                }
        }
    }

}
