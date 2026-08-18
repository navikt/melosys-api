package no.nav.melosys.saksflyt.e2e.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.time.LocalDateTime
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor

/**
 * Dekker vente-kontrakten på `/internal/e2e/process-instances/await`.
 *
 * Poenget med `after`-markøren: en prosessinstans som ble registrert FØR handlingen kalleren
 * venter på, skal ikke kunne oppfylle ventingen. Uten markør gjør den nettopp det — det er
 * racet som gjorde e2e-tester flaky, og det er bevisst bevart for de kallstedene som ikke er
 * migrert ennå.
 */
class E2ESupportControllerAwaitTest {

    private val prosessinstansRepository = mockk<ProsessinstansRepository>()
    private val taskExecutor = mockk<ThreadPoolTaskExecutor>()
    private val threadPoolExecutor = mockk<ThreadPoolExecutor>()

    private val controller = E2ESupportController(
        cacheManager = null,
        prosessinstansRepository = prosessinstansRepository,
        taskExecutor = taskExecutor,
        e2eTestDataService = mockk(),
        initialSettlingDelayMs = 0
    )

    init {
        every { taskExecutor.activeCount } returns 0
        every { taskExecutor.threadPoolExecutor } returns threadPoolExecutor
        every { threadPoolExecutor.queue } returns LinkedBlockingQueue()
    }

    @Test
    fun `markør - ferdig instans fra før markøren oppfyller ikke ventingen`() {
        val markør = LocalDateTime.now()
        girInstanser(ferdigInstans(registrert = markør.minusSeconds(5)))

        val svar = ventMedMarkør(markør)

        svar.statusCode shouldBe HttpStatus.REQUEST_TIMEOUT
        svar.body!!["status"] shouldBe "TIMEOUT"
        svar.body!!["newInstances"] shouldBe 0
    }

    @Test
    fun `markør - ny ferdig instans etter markøren fullfører ventingen`() {
        val markør = LocalDateTime.now()
        girInstanser(
            ferdigInstans(registrert = markør.minusSeconds(5)),
            ferdigInstans(registrert = markør.plusNanos(1))
        )

        val svar = ventMedMarkør(markør)

        svar.statusCode shouldBe HttpStatus.OK
        svar.body!!["status"] shouldBe "COMPLETED"
        svar.body!!["newInstances"] shouldBe 1
    }

    @Test
    fun `markør - ny instans som ikke er ferdig fullfører ikke ventingen`() {
        val markør = LocalDateTime.now()
        girInstanser(instans(ProsessStatus.KLAR, registrert = markør.plusNanos(1)))

        val svar = ventMedMarkør(markør)

        svar.statusCode shouldBe HttpStatus.REQUEST_TIMEOUT
        svar.body!!["notFinished"] shouldBe 1
    }

    @Test
    fun `markør - expectedNew krever like mange nye instanser`() {
        val markør = LocalDateTime.now()
        girInstanser(ferdigInstans(registrert = markør.plusNanos(1)))

        val svar = controller.awaitProcessInstances(
            timeoutSeconds = 1,
            expectedInstances = null,
            after = markør.toString(),
            expectedNew = 2
        )

        svar.statusCode shouldBe HttpStatus.REQUEST_TIMEOUT
        svar.body!!["message"].toString() shouldBe
            "Timeout after 1s: only 1 of 2 expected new process instance(s) were registered after $markør"
    }

    @Test
    fun `markør - feilet instans rapporteres som FAILED`() {
        val markør = LocalDateTime.now()
        girInstanser(instans(ProsessStatus.FEILET, registrert = markør.plusNanos(1)))

        val svar = ventMedMarkør(markør)

        svar.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        svar.body!!["status"] shouldBe "FAILED"
    }

    @Test
    fun `markør med expectedNew 0 er en ren tømming - krever ingen ny instans`() {
        val markør = LocalDateTime.now()
        girInstanser(ferdigInstans(registrert = markør.minusSeconds(5)))

        val svar = controller.awaitProcessInstances(
            timeoutSeconds = 1,
            expectedInstances = null,
            after = markør.toString(),
            expectedNew = 0
        )

        svar.statusCode shouldBe HttpStatus.OK
        svar.body!!["newInstances"] shouldBe 0
    }

    @Test
    fun `markør med expectedNew 0 venter likevel på uferdig arbeid etter markøren`() {
        val markør = LocalDateTime.now()
        girInstanser(instans(ProsessStatus.KLAR, registrert = markør.plusNanos(1)))

        val svar = controller.awaitProcessInstances(
            timeoutSeconds = 1,
            expectedInstances = null,
            after = markør.toString(),
            expectedNew = 0
        )

        svar.statusCode shouldBe HttpStatus.REQUEST_TIMEOUT
    }

    @Test
    fun `uten markør - forrige stegs instans fullfører ventingen (bevart gammel oppførsel)`() {
        girInstanser(ferdigInstans(registrert = LocalDateTime.now().minusSeconds(5)))

        val svar = controller.awaitProcessInstances(timeoutSeconds = 1)

        svar.statusCode shouldBe HttpStatus.OK
        svar.body!!["status"] shouldBe "COMPLETED"
    }

    @Test
    fun `markør - instans som registreres MENS vi venter fullfører ventingen (selve racet)`() {
        val markør = LocalDateTime.now()
        val nyInstans = ferdigInstans(registrert = markør.plusNanos(1))
        // Første oppslag: bare forrige stegs instans finnes — handlingens egen er ikke registrert ennå.
        // Det er nøyaktig situasjonen der den gamle kontrakten svarte COMPLETED.
        every { prosessinstansRepository.findAll() } returnsMany listOf(
            listOf(ferdigInstans(registrert = markør.minusSeconds(5))),
            listOf(ferdigInstans(registrert = markør.minusSeconds(5))),
            listOf(ferdigInstans(registrert = markør.minusSeconds(5)), nyInstans)
        )

        val svar = ventMedMarkør(markør)

        svar.statusCode shouldBe HttpStatus.OK
        svar.body!!["newInstances"] shouldBe 1
    }

    @Test
    fun `markør - instans registrert på nøyaktig samme tidspunkt teller ikke som ny`() {
        val markør = LocalDateTime.now()
        girInstanser(ferdigInstans(registrert = markør))

        ventMedMarkør(markør).statusCode shouldBe HttpStatus.REQUEST_TIMEOUT
    }

    @Test
    fun `markør - feilet instans fra FØR markøren rapporteres fortsatt`() {
        val markør = LocalDateTime.now()
        girInstanser(instans(ProsessStatus.FEILET, registrert = markør.minusSeconds(5)))

        val svar = ventMedMarkør(markør)

        svar.statusCode shouldBe HttpStatus.INTERNAL_SERVER_ERROR
        svar.body!!["status"] shouldBe "FAILED"
    }

    @Test
    fun `blank after avvises i stedet for å falle stille tilbake til gammel kontrakt`() {
        avvises(after = "", expectedNew = 1) shouldContain "must not be blank"
    }

    @Test
    fun `ugyldig after avvises med 400, ikke 500`() {
        avvises(after = "tull") shouldContain "must be a local date-time"
    }

    @Test
    fun `ugyldig after ekkoes uten kontrolltegn og avkortet`() {
        // Meldingen både logges og returneres. Et `%0A` i `after` skal ikke kunne legge inn en
        // egen logglinje, og kallerens input skal ikke kunne blåse opp verken logg eller svar.
        val melding = avvises(after = "tull\nINFO injisert logglinje ${"x".repeat(200)}")

        melding shouldContain "must be a local date-time"
        melding.contains('\n') shouldBe false
        melding shouldContain "... (truncated)"
    }

    @Test
    fun `after med tidssone avvises - containerklokka er ikke kallerens klokke`() {
        avvises(after = "2026-07-31T14:36:00Z") shouldContain "must be a local date-time"
    }

    @Test
    fun `expectedNew uten after avvises`() {
        avvises(expectedNew = 3) shouldContain "requires 'after'"
    }

    @Test
    fun `negativ expectedNew avvises`() {
        avvises(after = LocalDateTime.now().toString(), expectedNew = -1) shouldContain "must not be negative"
    }

    @Test
    fun `after kombinert med expectedInstances avvises - de teller forskjellige ting`() {
        avvises(after = LocalDateTime.now().toString(), expectedInstances = 3) shouldContain "mutually exclusive"
    }

    private fun avvises(after: String? = null, expectedNew: Int? = null, expectedInstances: Int? = null): String {
        val svar = controller.awaitProcessInstances(
            timeoutSeconds = 1,
            expectedInstances = expectedInstances,
            after = after,
            expectedNew = expectedNew
        )
        svar.statusCode shouldBe HttpStatus.BAD_REQUEST
        return svar.body!!["message"].toString()
    }

    private fun ventMedMarkør(markør: LocalDateTime) =
        controller.awaitProcessInstances(
            timeoutSeconds = 1,
            expectedInstances = null,
            after = markør.toString(),
            expectedNew = null
        )

    private fun girInstanser(vararg instanser: Prosessinstans) {
        every { prosessinstansRepository.findAll() } returns instanser.toList()
    }

    private fun ferdigInstans(registrert: LocalDateTime) = instans(ProsessStatus.FERDIG, registrert)

    private fun instans(status: ProsessStatus, registrert: LocalDateTime) = Prosessinstans.forTest {
        this.status = status
        this.registrertDato = registrert
    }
}
