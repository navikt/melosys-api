package no.nav.melosys.service.soknad

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
class DigitalSøknadMottattConsumerTest {

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    private lateinit var digitalSøknadMottattConsumer: DigitalSøknadMottattConsumer

    private var antallPauser = 0

    @BeforeEach
    fun setUp() {
        antallPauser = 0
        digitalSøknadMottattConsumer = DigitalSøknadMottattConsumer(
            prosessinstansService, konsumeringDelay = { antallPauser++ }
        )
    }

    @Test
    fun `mottaSkjemaMelding skal opprette prosessinstans for digital søknad`() {
        val melding = SkjemaMottattMelding(UUID.randomUUID(), listOf(UUID.randomUUID()))
        val consumerRecord = ConsumerRecord("topic", 0, 0, "key", melding)

        every { prosessinstansService.opprettProsessinstansDigitalSøknadMottatt(melding) } just Runs

        digitalSøknadMottattConsumer.mottaSkjemaMelding(consumerRecord, emptyMap())

        verify(exactly = 1) { prosessinstansService.opprettProsessinstansDigitalSøknadMottatt(melding) }
        antallPauser shouldBe 1
    }
}
