package no.nav.melosys.service.soknad

import io.getunleash.Unleash
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.Runs
import io.mockk.verify
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.SkjemaMottattMelding
import no.nav.melosys.saksflytapi.ProsessinstansService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
class SkjemaMottattConsumerTest {

    @MockK
    private lateinit var unleash: Unleash

    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    private lateinit var skjemaMottattConsumer: SkjemaMottattConsumer

    @BeforeEach
    fun setUp() {
        skjemaMottattConsumer = SkjemaMottattConsumer(unleash, prosessinstansService)
    }

    @Test
    fun `mottaSkjemaMelding skal opprette prosessinstans når toggle er aktivert`() {
        val skjemaId = UUID.randomUUID()
        val melding = SkjemaMottattMelding(skjemaId)
        val consumerRecord = ConsumerRecord<String, SkjemaMottattMelding>("topic", 0, 0, "key", melding)

        every { unleash.isEnabled(ToggleName.MELOSYS_SKJEMA_MOTTATT_CONSUMER) } returns true
        every { prosessinstansService.opprettProsessinstansMelosysSkjemaMottatt(skjemaId) } just Runs

        skjemaMottattConsumer.mottaSkjemaMelding(consumerRecord, emptyMap())

        verify { prosessinstansService.opprettProsessinstansMelosysSkjemaMottatt(skjemaId) }
    }

    @Test
    fun `mottaSkjemaMelding skal ikke opprette prosessinstans når toggle er deaktivert`() {
        val skjemaId = UUID.randomUUID()
        val melding = SkjemaMottattMelding(skjemaId)
        val consumerRecord = ConsumerRecord<String, SkjemaMottattMelding>("topic", 0, 0, "key", melding)

        every { unleash.isEnabled(ToggleName.MELOSYS_SKJEMA_MOTTATT_CONSUMER) } returns false

        skjemaMottattConsumer.mottaSkjemaMelding(consumerRecord, emptyMap())

        verify(exactly = 0) { prosessinstansService.opprettProsessinstansMelosysSkjemaMottatt(any()) }
    }
}
