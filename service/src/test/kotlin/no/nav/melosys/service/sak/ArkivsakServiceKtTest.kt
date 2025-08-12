package no.nav.melosys.service.sak

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Tema
import no.nav.melosys.integrasjon.sak.SakConsumerInterface
import no.nav.melosys.integrasjon.sak.SakDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ArkivsakServiceKtTest {
    @MockK
    private lateinit var sakConsumer: SakConsumerInterface

    private lateinit var arkivsakService: ArkivsakService

    @BeforeEach
    fun setup() {
        arkivsakService = ArkivsakService(sakConsumer)
    }

    @Test
    fun `opprettSakForBruker_behandlingstypeFørstegang_temaMed`() {
        val saksnummer = "MEL-123"
        val tema = Tema.MED
        val aktørID = "123123123"
        val sakID = 1111L

        val sakDto = SakDto().apply {
            id = sakID
        }
        every { sakConsumer.opprettSak(any()) } returns sakDto

        val opprettetSakID = arkivsakService.opprettSakForBruker(saksnummer, tema, aktørID)

        opprettetSakID shouldBe sakID
        verify { sakConsumer.opprettSak(any()) }

        val capturedSakDto = slot<SakDto>()
        verify { sakConsumer.opprettSak(capture(capturedSakDto)) }
        capturedSakDto.captured.tema shouldBe Tema.MED.kode
    }

    @Test
    fun `opprettSakForBruker_behandlingstypeRegistreringUnntak_temaUfm`() {
        val saksnummer = "MEL-123"
        val tema = Tema.UFM
        val aktørID = "123123123"
        val sakID = 1111L

        val sakDto = SakDto().apply {
            id = sakID
        }
        every { sakConsumer.opprettSak(any()) } returns sakDto

        val opprettetSakID = arkivsakService.opprettSakForBruker(saksnummer, tema, aktørID)

        opprettetSakID shouldBe sakID
        verify { sakConsumer.opprettSak(any()) }

        val capturedSakDto = slot<SakDto>()
        verify { sakConsumer.opprettSak(capture(capturedSakDto)) }
        capturedSakDto.captured.tema shouldBe Tema.UFM.kode
    }

    @Test
    fun `opprettSakForVirksomhet_behandlingstypeFørstegang_temaMed`() {
        val saksnummer = "MEL-123"
        val tema = Tema.MED
        val orgId = "123123123"
        val sakID = 1111L

        val sakDto = SakDto().apply {
            id = sakID
        }
        every { sakConsumer.opprettSak(any()) } returns sakDto

        val opprettetSakID = arkivsakService.opprettSakForVirksomhet(saksnummer, tema, orgId)

        opprettetSakID shouldBe sakID
        verify { sakConsumer.opprettSak(any()) }

        val capturedSakDto = slot<SakDto>()
        verify { sakConsumer.opprettSak(capture(capturedSakDto)) }
        capturedSakDto.captured.tema shouldBe Tema.MED.kode
    }

    @Test
    fun `opprettSakForVirksomhet_behandlingstypeRegistreringUnntak_temaUfm`() {
        val saksnummer = "MEL-123"
        val tema = Tema.UFM
        val orgId = "123123123"
        val sakID = 1111L

        val sakDto = SakDto().apply {
            id = sakID
        }
        every { sakConsumer.opprettSak(any()) } returns sakDto

        val opprettetSakID = arkivsakService.opprettSakForVirksomhet(saksnummer, tema, orgId)

        opprettetSakID shouldBe sakID
        verify { sakConsumer.opprettSak(any()) }

        val capturedSakDto = slot<SakDto>()
        verify { sakConsumer.opprettSak(capture(capturedSakDto)) }
        capturedSakDto.captured.tema shouldBe Tema.UFM.kode
    }
}
