package no.nav.melosys.sikkerhet.context

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.melosys.exception.TekniskException
import org.junit.jupiter.api.*
import org.slf4j.LoggerFactory
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThreadLocalAccessInfoTest {

    private val listAppender = ListAppender<ILoggingEvent>()

    @BeforeAll
    fun setUp() {
        val logger = LoggerFactory.getLogger("no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo") as Logger
        listAppender.setContext(LoggerFactory.getILoggerFactory() as LoggerContext)
        logger.level = Level.WARN
        logger.addAppender(listAppender)
        listAppender.start()
    }

    @AfterAll
    fun tearDown() {
        listAppender.stop()
    }

    @AfterEach
    fun afterEach() {
        listAppender.list.clear()
    }

    @Test
    fun `isProcessCall med uregistrert call skal logge og returnere true`() {
        ThreadLocalAccessInfo.shouldUseSystemToken() shouldBe true
        listAppender.list.shouldHaveSize(1)
            .single().message shouldContain "Call have not been registrert from RestController or Prosess"
    }

    @Test
    fun `isProcessCall med registrert prosess call skal returnere true`() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "Test")

        ThreadLocalAccessInfo.shouldUseSystemToken() shouldBe true
        listAppender.list.shouldBeEmpty()

        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    @Test
    fun `isProcessCall med registrert web call skal returnere false`() {
        ThreadLocalAccessInfo.beforeControllerRequest("test", false)

        ThreadLocalAccessInfo.shouldUseSystemToken() shouldBe false
        listAppender.list.shouldBeEmpty()

        ThreadLocalAccessInfo.afterControllerRequest("test")
    }

    @Test
    fun `isProcessCall med registrert admin call skal returnere true`() {
        ThreadLocalAccessInfo.beforeControllerRequest("Test", true)

        ThreadLocalAccessInfo.shouldUseSystemToken() shouldBe true

        ThreadLocalAccessInfo.afterControllerRequest("Test")
    }

    @Test
    fun `isFrontendCall med uregistrert call skal returnere false`() {
        ThreadLocalAccessInfo.shouldUseOidcToken() shouldBe false
    }

    @Test
    fun `isFrontendCall med registrert call skal returnere true`() {
        ThreadLocalAccessInfo.beforeControllerRequest("Test", false)

        ThreadLocalAccessInfo.shouldUseOidcToken() shouldBe true

        ThreadLocalAccessInfo.afterControllerRequest("Test")
    }

    @Test
    fun `isFrontendCall med registrert web call skal returnere false`() {
        val uuid = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(uuid, "Test")

        ThreadLocalAccessInfo.shouldUseOidcToken() shouldBe false

        ThreadLocalAccessInfo.afterExecuteProcess(uuid)
    }

    @Test
    fun `executeProcess skal utføre lambda funksjon`() {
        val commands = mutableListOf<String>()
        val processToBeExecuted = Runnable {
            commands.add("executed")
            ThreadLocalAccessInfo.shouldUseSystemToken() shouldBe true
        }

        ThreadLocalAccessInfo.executeProcess("Test", processToBeExecuted)

        listAppender.list.shouldBeEmpty()
        commands.shouldHaveSize(1)
            .single() shouldBe "executed"
    }

    @Test
    fun `executeProcess skal håndtere feil og kaste TekniskException`() {
        val processId = UUID.randomUUID()
        val processToBeExecuted = Runnable {
            throw TekniskException("Some Error")
        }

        val exception = shouldThrow<TekniskException> {
            ThreadLocalAccessInfo.executeProcess(processId, "Test", processToBeExecuted)
        }
        exception.message shouldContain "Some Error"

        // Om ikke exception blir korrekt håntert over vil denne feile
        ThreadLocalAccessInfo.executeProcess(processId, "Test") {}
    }
}
