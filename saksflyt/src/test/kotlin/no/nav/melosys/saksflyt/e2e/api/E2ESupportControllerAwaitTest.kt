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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
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
            after = markør.toString(),
            expectedNew = 0
        )

        svar.statusCode shouldBe HttpStatus.REQUEST_TIMEOUT
    }

    @Test
    fun `uten markør - instanser eldre enn vinduet teller ikke som arbeid`() {
        // Bare gamle instanser (utenfor 60-sekundersvinduet) og ingen aktivitet: da har
        // serveren ikke sett noe arbeid, og skal ikke påstå at kallerens venting er oppfylt.
        girInstanser(ferdigInstans(registrert = LocalDateTime.now().minusSeconds(120)))

        val svar = controller.awaitProcessInstances(timeoutSeconds = 1)

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

        val svar = ventMedMarkør(markør)

        svar.statusCode shouldBe HttpStatus.REQUEST_TIMEOUT
        // Ikke bare «ikke ferdig» — den skal ikke telles som ny i det hele tatt.
        svar.body!!["newInstances"] shouldBe 0
    }

    @Test
    fun `markør fra framtiden avvises - kan ikke stamme fra denne serveren`() {
        avvises(after = LocalDateTime.now().plusMinutes(5).toString()) shouldContain "is in the future"
    }

    @Test
    fun `gammel markør er lovlig - tømmingen bruker en markør tatt før hele testen`() {
        val gammelMarkør = LocalDateTime.now().minusMinutes(10)
        girInstanser(ferdigInstans(registrert = gammelMarkør.plusMinutes(1)))

        val svar = controller.awaitProcessInstances(
            timeoutSeconds = 1,
            after = gammelMarkør.toString(),
            expectedNew = 0
        )

        svar.statusCode shouldBe HttpStatus.OK
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
    fun `expectedInstances avvises i stedet for å bli ignorert av Spring`() {
        // Parameteren er fjernet. Ignoreres den, faller kalleren stille tilbake til legacy-kontrakten
        // — nøyaktig den stille degraderingen resten av endepunktet avviser.
        avvises(expectedInstances = 2) shouldContain "'expectedInstances' is gone"
    }

    @Test
    fun `timeoutSeconds utenfor intervallet avvises`() {
        avvises(timeoutSeconds = 0) shouldContain "'timeoutSeconds' must be between"
        avvises(timeoutSeconds = -5) shouldContain "'timeoutSeconds' must be between"
        avvises(timeoutSeconds = 3600) shouldContain "'timeoutSeconds' must be between"
    }

    @Test
    fun `utypet query-parameter gir 400, ikke 500`() {
        val svar = controller.håndterTypefeil(
            MethodArgumentTypeMismatchException("abc", Long::class.java, "timeoutSeconds", mockk(), null)
        )

        svar.statusCode shouldBe HttpStatus.BAD_REQUEST
        svar.body!!["message"].toString() shouldContain "timeoutSeconds"
    }

    @Test
    fun `gammel markør med expectedNew over 0 gir advarsel i svaret, ikke bare i serverloggen`() {
        // En gjenbrukt markør kan ikke avvises (en treg handling gir legitimt gammel markør), men
        // serverloggen er usynlig i CI. Advarselen må følge svaret for å nå testforfatteren.
        val gammelMarkør = LocalDateTime.now().minusMinutes(10)
        girInstanser(ferdigInstans(registrert = gammelMarkør.plusMinutes(1)))

        val svar = controller.awaitProcessInstances(
            timeoutSeconds = 1,
            after = gammelMarkør.toString(),
            expectedNew = 1
        )

        svar.statusCode shouldBe HttpStatus.OK
        svar.body!!["warning"].toString() shouldContain "hent en ny markør"
    }

    @Test
    fun `tømming med gammel markør advarer ikke - den er per design gammel`() {
        val gammelMarkør = LocalDateTime.now().minusMinutes(10)
        girInstanser(ferdigInstans(registrert = gammelMarkør.plusMinutes(1)))

        val svar = controller.awaitProcessInstances(
            timeoutSeconds = 1,
            after = gammelMarkør.toString(),
            expectedNew = 0
        )

        svar.body!!.containsKey("warning") shouldBe false
    }

    @Test
    fun `fersk markør advarer ikke`() {
        val markør = LocalDateTime.now()
        girInstanser(ferdigInstans(registrert = markør.plusSeconds(1)))

        val svar = ventMedMarkør(markør)

        svar.body!!.containsKey("warning") shouldBe false
    }

    private fun avvises(
        after: String? = null,
        expectedNew: Int? = null,
        expectedInstances: Int? = null,
        timeoutSeconds: Long = 1
    ): String {
        val svar = controller.awaitProcessInstances(
            timeoutSeconds = timeoutSeconds,
            after = after,
            expectedNew = expectedNew,
            expectedInstances = expectedInstances
        )
        svar.statusCode shouldBe HttpStatus.BAD_REQUEST
        return svar.body!!["message"].toString()
    }

    private fun ventMedMarkør(markør: LocalDateTime) =
        controller.awaitProcessInstances(
            timeoutSeconds = 1,
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
