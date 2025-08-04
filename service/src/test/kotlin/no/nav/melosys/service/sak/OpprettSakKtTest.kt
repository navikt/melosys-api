package no.nav.melosys.service.sak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.dto.PeriodeDto
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates.named
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OpprettSakKtTest {
    @MockK
    private lateinit var prosessinstansService: ProsessinstansService

    @MockK
    private lateinit var lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService

    @MockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    private val random = EasyRandom(getRandomConfig())

    private lateinit var opprettSak: OpprettSak

    @BeforeEach
    fun setUp() {
        opprettSak = OpprettSak(prosessinstansService, saksbehandlingRegler, lovligeKombinasjonerSaksbehandlingService)

        // Mock setups for validation methods
        every { lovligeKombinasjonerSaksbehandlingService.validerOpprettelseOgEndring(any(), any(), any(), any(), any()) } returns Unit
        every { prosessinstansService.opprettNySakOgBehandling(any()) } returns Unit
        every { saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any(), any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any(), any(), any()) } returns false
    }

    @Test
    fun `lagNySak_EU_EOS_oppretterProsess`() {
        val opprettSakDto = random.nextObject(OpprettSakDto::class.java)
        opprettSakDto.hovedpart = Aktoersroller.BRUKER
        opprettSakDto.sakstype = Sakstyper.EU_EOS
        opprettSakDto.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
        opprettSakDto.behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        opprettSakDto.behandlingstype = Behandlingstyper.HENVENDELSE
        opprettSakDto.soknadDto = opprettSoknadDto()

        opprettSak.opprettNySakOgBehandling(opprettSakDto)

        val capturedRequest = slot<no.nav.melosys.saksflytapi.journalfoering.OpprettSakRequest>()
        verify { prosessinstansService.opprettNySakOgBehandling(capture(capturedRequest)) }
        capturedRequest.captured shouldBe opprettSakDto.tilOpprettSakRequest()
    }

    @Test
    fun `lagNySak_TRYGDEAVTALE_oppretterProsess`() {
        val opprettSakDto = random.nextObject(OpprettSakDto::class.java)
        opprettSakDto.hovedpart = Aktoersroller.BRUKER
        opprettSakDto.sakstype = Sakstyper.TRYGDEAVTALE
        opprettSakDto.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
        opprettSakDto.behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        opprettSakDto.behandlingstype = Behandlingstyper.HENVENDELSE

        opprettSak.opprettNySakOgBehandling(opprettSakDto)

        verify { prosessinstansService.opprettNySakOgBehandling(opprettSakDto.tilOpprettSakRequest()) }
    }

    @Test
    fun `lagNySak_FTRL_oppretterProsess`() {
        val opprettSakDto = random.nextObject(OpprettSakDto::class.java)
        opprettSakDto.hovedpart = Aktoersroller.BRUKER
        opprettSakDto.sakstype = Sakstyper.FTRL
        opprettSakDto.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
        opprettSakDto.behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        opprettSakDto.behandlingstype = Behandlingstyper.HENVENDELSE

        opprettSak.opprettNySakOgBehandling(opprettSakDto)

        val capturedRequest = slot<no.nav.melosys.saksflytapi.journalfoering.OpprettSakRequest>()
        verify { prosessinstansService.opprettNySakOgBehandling(capture(capturedRequest)) }
        capturedRequest.captured shouldBe opprettSakDto.tilOpprettSakRequest()
    }

    @Test
    fun `lagNySak_mottaksdatoMangler_feiler`() {
        val opprettSakDto = random.nextObject(OpprettSakDto::class.java)
        opprettSakDto.hovedpart = Aktoersroller.BRUKER
        opprettSakDto.sakstype = Sakstyper.FTRL
        opprettSakDto.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
        opprettSakDto.behandlingstema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        opprettSakDto.behandlingstype = Behandlingstyper.HENVENDELSE
        opprettSakDto.mottaksdato = null

        val exception = shouldThrow<FunksjonellException> {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        }
        exception.message shouldContain "Mottaksdato"
    }

    @Test
    fun `lagNySak_årsakFritekstMedFeilType_feiler`() {
        val opprettSakDto = random.nextObject(OpprettSakDto::class.java)
        opprettSakDto.behandlingsaarsakFritekst = "Fritekst"
        opprettSakDto.behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD

        val exception = shouldThrow<FunksjonellException> {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        }
        exception.message shouldContain "Kan ikke lagre fritekst som årsak når årsakstype"
    }

    private fun opprettSoknadDto(): SøknadDto {
        val søknadDto = SøknadDto()
        søknadDto.periode = PeriodeDto(LocalDate.now().minusMonths(4), LocalDate.now().minusMonths(3))
        søknadDto.land = SoeknadslandDto.av(Landkoder.DE)
        return søknadDto
    }

    companion object {
        private fun getRandomConfig(): EasyRandomParameters {
            return EasyRandomParameters().collectionSizeRange(1, 4)
                .randomize(PeriodeDto::class.java) { PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1)) }
                .excludeField(named("behandlingsaarsakFritekst"))
                .stringLengthRange(2, 4)
        }
    }
}
