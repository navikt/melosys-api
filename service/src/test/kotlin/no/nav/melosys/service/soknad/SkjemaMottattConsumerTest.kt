package no.nav.melosys.service.soknad

import io.getunleash.Unleash
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.Runs
import io.mockk.verify
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.skjema.types.kafka.SkjemaMottattMelding
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.sak.SkjemaSakMappingService
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

    @MockK
    private lateinit var skjemaSakMappingService: SkjemaSakMappingService

    private lateinit var skjemaMottattConsumer: SkjemaMottattConsumer

    @BeforeEach
    fun setUp() {
        skjemaMottattConsumer = SkjemaMottattConsumer(unleash, prosessinstansService, skjemaSakMappingService)
    }

    @Test
    fun `mottaSkjemaMelding skal opprette ny sak når toggle er aktivert og ingen eksisterende sak`() {
        val skjemaId = UUID.randomUUID()
        val melding = SkjemaMottattMelding(skjemaId)
        val consumerRecord = ConsumerRecord<String, SkjemaMottattMelding>("topic", 0, 0, "key", melding)

        every { unleash.isEnabled(ToggleName.MELOSYS_SKJEMA_MOTTATT_CONSUMER) } returns true
        every { skjemaSakMappingService.finnSaksnummerForGyldigSak(any()) } returns null
        every { prosessinstansService.`opprettProsessinstansMelosysSøknadMottatt`(melding) } just Runs

        skjemaMottattConsumer.mottaSkjemaMelding(consumerRecord, emptyMap())

        verify { prosessinstansService.`opprettProsessinstansMelosysSøknadMottatt`(melding) }
        verify(exactly = 0) { prosessinstansService.opprettProsessinstansEksisterendeDigitalSøknad(any(), any()) }
    }

    @Test
    fun `mottaSkjemaMelding skal bruke eksisterende sak når mapping finnes`() {
        val skjemaId = UUID.randomUUID()
        val melding = SkjemaMottattMelding(skjemaId)
        val consumerRecord = ConsumerRecord<String, SkjemaMottattMelding>("topic", 0, 0, "key", melding)

        every { unleash.isEnabled(ToggleName.MELOSYS_SKJEMA_MOTTATT_CONSUMER) } returns true
        every { skjemaSakMappingService.finnSaksnummerForGyldigSak(any()) } returns "MEL-1"
        every { prosessinstansService.opprettProsessinstansEksisterendeDigitalSøknad(melding, "MEL-1") } just Runs

        skjemaMottattConsumer.mottaSkjemaMelding(consumerRecord, emptyMap())

        verify { prosessinstansService.opprettProsessinstansEksisterendeDigitalSøknad(melding, "MEL-1") }
        verify(exactly = 0) { prosessinstansService.`opprettProsessinstansMelosysSøknadMottatt`(any()) }
    }

    @Test
    fun `mottaSkjemaMelding skal ikke opprette prosessinstans når toggle er deaktivert`() {
        val skjemaId = UUID.randomUUID()
        val melding = SkjemaMottattMelding(skjemaId)
        val consumerRecord = ConsumerRecord<String, SkjemaMottattMelding>("topic", 0, 0, "key", melding)

        every { unleash.isEnabled(ToggleName.MELOSYS_SKJEMA_MOTTATT_CONSUMER) } returns false

        skjemaMottattConsumer.mottaSkjemaMelding(consumerRecord, emptyMap())

        verify(exactly = 0) { prosessinstansService.`opprettProsessinstansMelosysSøknadMottatt`(any<SkjemaMottattMelding>()) }
    }
}
