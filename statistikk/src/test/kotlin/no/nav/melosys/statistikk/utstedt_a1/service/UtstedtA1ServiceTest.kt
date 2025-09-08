package no.nav.melosys.statistikk.utstedt_a1.service

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.service.LandvelgerService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1AivenProducer
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.A1TypeUtstedelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class UtstedtA1ServiceTest {
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val landvelgerService = mockk<LandvelgerService>()
    private val utstedtA1AivenProducer = mockk<UtstedtA1AivenProducer>()

    private lateinit var utstedtA1Service: UtstedtA1Service

    @BeforeEach
    fun setUp() {
        utstedtA1Service = UtstedtA1Service(utstedtA1AivenProducer, behandlingsresultatService, landvelgerService)
    }

    @Test
    fun `send melding om utstedt A1 på Aiven skal produsere korrekt melding`() {
        val meldingSlot = slot<UtstedtA1Melding>()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresultat()
        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.SE)
        every { utstedtA1AivenProducer.produserMelding(capture(meldingSlot)) } answers { firstArg() }


        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID)


        verify { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }
        verify { landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) }
        verify { utstedtA1AivenProducer.produserMelding(any()) }

        meldingSlot.captured.shouldNotBeNull().run {
            serienummer shouldBe "MEL-123123"
            utsendtTilLand shouldBe "SE"
            artikkel shouldBe Lovvalgsbestemmelse.ART_12_1
            typeUtstedelse shouldBe A1TypeUtstedelse.FØRSTEGANG
        }
    }

    @Test
    fun `send melding om utstedt A1 ved avslag skal ikke sende melding`() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresultat(true, lagBehandling())


        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID)


        verify { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }
        verify(exactly = 0) { landvelgerService.hentUtenlandskTrygdemyndighetsland(any()) }
        verify(exactly = 0) { utstedtA1AivenProducer.produserMelding(any()) }
    }

    @Test
    fun `send melding om utstedt A1 med artikkel 13 skal ha tom landkode`() {
        val meldingSlot = slot<UtstedtA1Melding>()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresultat(
            false,
            lagBehandling(),
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A
        )
        every { utstedtA1AivenProducer.produserMelding(capture(meldingSlot)) } answers { firstArg() }

        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID)

        verify { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }
        verify(exactly = 0) { landvelgerService.hentUtenlandskTrygdemyndighetsland(any()) }
        verify { utstedtA1AivenProducer.produserMelding(any()) }

        meldingSlot.captured.shouldNotBeNull().run {
            serienummer shouldBe "MEL-123123"
            utsendtTilLand.shouldBeNull()
            artikkel shouldBe Lovvalgsbestemmelse.ART_13_1
            typeUtstedelse shouldBe A1TypeUtstedelse.FØRSTEGANG
        }
    }

    @Test
    fun `send melding om utstedt A1 med artikkel 11 skal ha tom landkode`() {
        val meldingSlot = slot<UtstedtA1Melding>()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresultat(
            false,
            lagBehandling(),
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
        )
        every { utstedtA1AivenProducer.produserMelding(capture(meldingSlot)) } answers { firstArg() }

        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID)

        verify { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }
        verify(exactly = 0) { landvelgerService.hentUtenlandskTrygdemyndighetsland(any()) }
        verify { utstedtA1AivenProducer.produserMelding(any()) }

        meldingSlot.captured.shouldNotBeNull().run {
            serienummer shouldBe "MEL-123123"
            utsendtTilLand.shouldBeNull()
            artikkel shouldBe Lovvalgsbestemmelse.ART_11_3_a
            typeUtstedelse shouldBe A1TypeUtstedelse.FØRSTEGANG
        }
    }

    @Test
    fun `send melding om utstedt A1 med artikkel 12 og tilleggsbestemmelse artikkel 11 skal ha landkode`() {
        val behandlingsresultat = lagBehandlingsresultat(false, lagBehandling(), Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1).apply {
            lovvalgsperioder.first().tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
        }
        val meldingSlot = slot<UtstedtA1Melding>()

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat
        every { landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) } returns listOf(Land_iso2.SE)
        every { utstedtA1AivenProducer.produserMelding(capture(meldingSlot)) } answers { firstArg() }

        utstedtA1Service.sendMeldingOmUtstedtA1(BEHANDLING_ID)

        verify { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }
        verify { landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID) }
        verify { utstedtA1AivenProducer.produserMelding(any()) }


        meldingSlot.captured.shouldNotBeNull().run {
            serienummer shouldBe "MEL-123123"
            utsendtTilLand shouldBe "SE"
            artikkel shouldBe Lovvalgsbestemmelse.ART_12_1
            typeUtstedelse shouldBe A1TypeUtstedelse.FØRSTEGANG
        }
    }

    private fun lagBehandling(behandlingsstatus: Behandlingsstatus = Behandlingsstatus.AVSLUTTET) =
        Behandling.forTest {
            id = BEHANDLING_ID
            fagsak {
                saksnummer = "MEL-123"
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                status = Saksstatuser.OPPRETTET
                medBruker()
            }
            status = behandlingsstatus
        }

    private fun lagBehandlingsresultat(
        erAvslag: Boolean = false,
        behandling: Behandling = lagBehandling(),
        bestemmelse: LovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
    ) = Behandlingsresultat().apply {
        id = BEHANDLING_ID
        this.behandling = behandling
        lovvalgsperioder = setOf(
            Lovvalgsperiode().apply {
                this.bestemmelse = bestemmelse
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                fom = LocalDate.now()
                tom = LocalDate.now().plusMonths(3)
            }
        )
        vedtakMetadata = VedtakMetadata().apply {
            vedtaksdato = Instant.now()
            vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        }
        type = if (erAvslag) {
            Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL
        } else {
            Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        }
    }

    companion object {
        private const val BEHANDLING_ID = 123L
    }
}
