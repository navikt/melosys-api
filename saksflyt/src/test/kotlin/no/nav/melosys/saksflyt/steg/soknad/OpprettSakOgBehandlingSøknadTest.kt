package no.nav.melosys.saksflyt.steg.soknad

import tools.jackson.databind.json.JsonMapper
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.OpprettSakRequest
import no.nav.melosys.service.sak.SkjemaSakMappingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OpprettSakOgBehandlingSøknadTest {

    @MockK lateinit var fagsakService: FagsakService
    @MockK lateinit var persondataFasade: PersondataFasade
    @MockK lateinit var mottatteOpplysningerService: MottatteOpplysningerService
    @MockK lateinit var jsonMapper: JsonMapper
    @MockK lateinit var skjemaSakMappingService: SkjemaSakMappingService
    @MockK lateinit var behandlingService: BehandlingService

    private lateinit var opprettSakOgBehandlingSøknad: OpprettSakOgBehandlingSøknad
    private lateinit var prosessinstans: Prosessinstans

    private val fnr = "12345678901"
    private val aktørId = "1234567890123"
    private val orgnr = "123456789"
    private val juridiskEnhetOrgnr = "987654321"
    private val referanseId = "MEL-TEST123"
    private val behandlingId = 42L

    private val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
        fnr = this@OpprettSakOgBehandlingSøknadTest.fnr
        orgnr = this@OpprettSakOgBehandlingSøknadTest.orgnr
        juridiskEnhetOrgnr = this@OpprettSakOgBehandlingSøknadTest.juridiskEnhetOrgnr
        referanseId = this@OpprettSakOgBehandlingSøknadTest.referanseId
    }

    @BeforeEach
    fun setup() {
        opprettSakOgBehandlingSøknad = OpprettSakOgBehandlingSøknad(
            fagsakService, persondataFasade, mottatteOpplysningerService, jsonMapper,
            skjemaSakMappingService, behandlingService
        )

        prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SØKNADSDATA, søknadsdata)
        }

        every { skjemaSakMappingService.lagreMappinger(any(), any()) } just Runs
    }

    private fun mockFagsakOgBehandling(): Behandling {
        val fagsak = mockk<Fagsak>()
        val behandling = mockk<Behandling>(relaxed = true)

        every { persondataFasade.hentAktørIdForIdent(fnr) } returns aktørId
        every { fagsakService.nyFagsakOgBehandling(any()) } returns fagsak
        every { fagsak.hentAktivBehandling() } returns behandling
        every { fagsak.saksnummer } returns "MEL-1234"
        every { behandling.id } returns behandlingId
        every { behandling.status } returns Behandlingsstatus.OPPRETTET

        return behandling
    }

    private fun mockMottatteOpplysninger() {
        every { jsonMapper.writeValueAsString(søknadsdata) } returns """{"referanseId":"$referanseId"}"""
        every {
            mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(any(), any(), any(), any())
        } returns mockk<MottatteOpplysninger>()
    }

    @Test
    fun `inngangsSteg returnerer OPPRETT_SAK_OG_BEHANDLING_SØKNAD`() {
        opprettSakOgBehandlingSøknad.inngangsSteg() shouldBe ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_SØKNAD
    }

    @Test
    fun `utfør oppretter fagsak og behandling med korrekte verdier`() {
        mockFagsakOgBehandling()
        mockMottatteOpplysninger()
        val requestSlot = slot<OpprettSakRequest>()
        every { fagsakService.nyFagsakOgBehandling(capture(requestSlot)) } returns mockk<Fagsak>().also {
            every { it.hentAktivBehandling() } returns mockk<Behandling>(relaxed = true).also { b ->
                every { b.id } returns behandlingId
                every { b.status } returns Behandlingsstatus.OPPRETTET
            }
            every { it.saksnummer } returns "MEL-1234"
        }

        opprettSakOgBehandlingSøknad.utfør(prosessinstans)

        val capturedRequest = requestSlot.captured
        capturedRequest.aktørID shouldBe aktørId
        capturedRequest.sakstype shouldBe Sakstyper.EU_EOS
        capturedRequest.sakstema shouldBe Sakstemaer.MEDLEMSKAP_LOVVALG
        capturedRequest.behandlingstema shouldBe Behandlingstema.UTSENDT_ARBEIDSTAKER
        capturedRequest.behandlingstype shouldBe Behandlingstyper.FØRSTEGANG
        capturedRequest.behandlingsårsaktype shouldBe Behandlingsaarsaktyper.SØKNAD
    }

    @Test
    fun `utfør setter behandling på prosessinstans`() {
        val behandling = mockFagsakOgBehandling()
        mockMottatteOpplysninger()

        opprettSakOgBehandlingSøknad.utfør(prosessinstans)

        prosessinstans.behandling shouldBe behandling
    }

    @Test
    fun `utfør lagrer mottatte opplysninger på behandlingen`() {
        mockFagsakOgBehandling()
        mockMottatteOpplysninger()
        val soeknadSlot = slot<Soeknad>()

        every {
            mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
                eq(behandlingId), any(), capture(soeknadSlot), eq(referanseId)
            )
        } returns mockk<MottatteOpplysninger>()

        opprettSakOgBehandlingSøknad.utfør(prosessinstans)

        soeknadSlot.captured.shouldNotBeNull()

        verify(exactly = 1) {
            jsonMapper.writeValueAsString(søknadsdata)
            mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(
                behandlingId, any(), any(), referanseId
            )
        }
    }

    @Test
    fun `utfør lagrer skjema-sak-mapping`() {
        mockFagsakOgBehandling()
        mockMottatteOpplysninger()

        opprettSakOgBehandlingSøknad.utfør(prosessinstans)

        verify { skjemaSakMappingService.lagreMappinger(any(), eq("MEL-1234")) }
    }
}
