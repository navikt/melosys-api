package no.nav.melosys.saksflyt.steg.soknad

import tools.jackson.databind.json.JsonMapper
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.SkjemaSakMappingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class HåndterEksisterendeSakDigitalSøknadTest {

    @MockK lateinit var fagsakService: FagsakService
    @MockK lateinit var behandlingService: BehandlingService
    @MockK lateinit var behandlingsresultatService: BehandlingsresultatService
    @MockK lateinit var mottatteOpplysningerService: MottatteOpplysningerService
    @MockK lateinit var oppgaveService: OppgaveService
    @MockK lateinit var skjemaSakMappingService: SkjemaSakMappingService
    @MockK lateinit var jsonMapper: JsonMapper

    private lateinit var steg: HåndterEksisterendeSakDigitalSøknad

    private val saksnummer = "MEL-1234"
    private val behandlingId = 42L
    private val mottatteOpplysningerId = 99L

    private val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto()

    @BeforeEach
    fun setup() {
        steg = HåndterEksisterendeSakDigitalSøknad(
            fagsakService, behandlingService, behandlingsresultatService,
            mottatteOpplysningerService, oppgaveService, skjemaSakMappingService, jsonMapper
        )

        every { jsonMapper.writeValueAsString(søknadsdata) } returns """{"referanseId":"test"}"""
        every { skjemaSakMappingService.lagreMapping(any(), any(), any(), any(), any()) } just Runs
    }

    @Test
    fun `inngangsSteg returnerer HÅNDTER_EKSISTERENDE_SAK_SØKNAD`() {
        steg.inngangsSteg() shouldBe ProsessSteg.HÅNDTER_EKSISTERENDE_SAK_DIGITAL_SØKNAD
    }

    @Nested
    inner class ÅpenBehandlingUnderBehandling {

        @Test
        fun `setter status til VURDER_DOKUMENT og kaller toemBehandlingsresultat`() {
            val behandling = lagBehandling(Behandlingsstatus.UNDER_BEHANDLING)
            val fagsak = lagFagsakMedBehandling(behandling)
            val prosessinstans = lagProsessinstans()

            mockFagsakService(fagsak)
            mockEndreStatus()
            mockTømBehandlingsresultat()
            mockOppdaterMottatteOpplysninger()
            mockHentMottatteOpplysninger(behandlingId)

            steg.utfør(prosessinstans)

            verify { behandlingService.endreStatus(behandling, Behandlingsstatus.VURDER_DOKUMENT) }
            verify { behandlingsresultatService.tømBehandlingsresultat(behandlingId) }
            verify { mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(behandlingId, any(), any()) }
            verify { skjemaSakMappingService.lagreMapping(any(), any(), any(), any(), any()) }
            prosessinstans.behandling shouldBe behandling
        }
    }

    @Nested
    inner class ÅpenBehandlingAvventDokPart {

        @Test
        fun `setter status til VURDER_DOKUMENT og kaller toemBehandlingsresultat`() {
            val behandling = lagBehandling(Behandlingsstatus.AVVENT_DOK_PART)
            val fagsak = lagFagsakMedBehandling(behandling)
            val prosessinstans = lagProsessinstans()

            mockFagsakService(fagsak)
            mockEndreStatus()
            mockTømBehandlingsresultat()
            mockOppdaterMottatteOpplysninger()
            mockHentMottatteOpplysninger(behandlingId)

            steg.utfør(prosessinstans)

            verify { behandlingService.endreStatus(behandling, Behandlingsstatus.VURDER_DOKUMENT) }
            verify { behandlingsresultatService.tømBehandlingsresultat(behandlingId) }
        }
    }

    @Nested
    inner class ÅpenBehandlingOpprettet {

        @Test
        fun `oppdaterer kun mottatte opplysninger uten statusendring`() {
            val behandling = lagBehandling(Behandlingsstatus.OPPRETTET)
            val fagsak = lagFagsakMedBehandling(behandling)
            val prosessinstans = lagProsessinstans()

            mockFagsakService(fagsak)
            mockOppdaterMottatteOpplysninger()
            mockHentMottatteOpplysninger(behandlingId)

            steg.utfør(prosessinstans)

            verify(exactly = 0) { behandlingService.endreStatus(any<Behandling>(), any()) }
            verify(exactly = 0) { behandlingsresultatService.tømBehandlingsresultat(any()) }
            verify { mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(behandlingId, any(), any()) }
            verify { skjemaSakMappingService.lagreMapping(any(), any(), any(), any(), any()) }
            prosessinstans.behandling shouldBe behandling
        }
    }

    @Nested
    inner class ÅpenBehandlingVurderDokument {

        @Test
        fun `oppdaterer kun mottatte opplysninger uten statusendring eller reset`() {
            val behandling = lagBehandling(Behandlingsstatus.VURDER_DOKUMENT)
            val fagsak = lagFagsakMedBehandling(behandling)
            val prosessinstans = lagProsessinstans()

            mockFagsakService(fagsak)
            mockOppdaterMottatteOpplysninger()
            mockHentMottatteOpplysninger(behandlingId)

            steg.utfør(prosessinstans)

            verify(exactly = 0) { behandlingService.endreStatus(any<Behandling>(), any()) }
            verify(exactly = 0) { behandlingsresultatService.tømBehandlingsresultat(any()) }
            verify { mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(behandlingId, any(), any()) }
            prosessinstans.behandling shouldBe behandling
        }
    }

    @Nested
    inner class IkkeSøknadsbehandlingstype {

        @Test
        fun `aktiv behandling med annen type faller gjennom til opprettNyVurdering`() {
            val eksisterendeBehandling = lagBehandling(Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.HENVENDELSE)
            val fagsak = mockk<Fagsak>(relaxed = true)
            every { fagsak.saksnummer } returns saksnummer
            every { fagsak.finnAktivBehandlingIkkeÅrsavregning() } returns eksisterendeBehandling
            every { fagsak.finnBrukersAktørID() } returns "1234567890123"
            every { fagsak.finnVirksomhetsOrgnr() } returns "123456789"

            val nyBehandling = mockk<Behandling>(relaxed = true)
            every { nyBehandling.id } returns 99L
            val prosessinstans = lagProsessinstans()

            mockFagsakService(fagsak)
            every { behandlingService.nyBehandling(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns nyBehandling
            every { mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(any(), any(), any(), any()) } returns
                mockk<MottatteOpplysninger> { every { id } returns mottatteOpplysningerId }
            every { oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(any(), any(), any(), any(), any()) } just Runs

            steg.utfør(prosessinstans)

            verify { behandlingService.nyBehandling(fagsak, any(), eq(Behandlingstyper.NY_VURDERING), any(), any(), any(), any(), any(), any()) }
            prosessinstans.behandling shouldBe nyBehandling
        }
    }

    @Nested
    inner class IngenÅpenBehandling {

        @Test
        fun `oppretter ny vurdering med oppgave`() {
            val fagsak = lagFagsakUtenAktivBehandling()
            val nyBehandling = mockk<Behandling>(relaxed = true)
            val prosessinstans = lagProsessinstans()

            mockFagsakService(fagsak)

            every { nyBehandling.id } returns 99L
            every {
                behandlingService.nyBehandling(
                    eq(fagsak), eq(Behandlingsstatus.OPPRETTET), eq(Behandlingstyper.NY_VURDERING),
                    eq(Behandlingstema.UTSENDT_ARBEIDSTAKER), isNull(), isNull(), any(), any(), isNull()
                )
            } returns nyBehandling

            every {
                mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(eq(99L), any(), any(), any())
            } returns mockk<MottatteOpplysninger> { every { id } returns mottatteOpplysningerId }

            every { oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(any(), any(), any(), any(), any()) } just Runs

            steg.utfør(prosessinstans)

            verify { behandlingService.nyBehandling(fagsak, Behandlingsstatus.OPPRETTET, Behandlingstyper.NY_VURDERING, Behandlingstema.UTSENDT_ARBEIDSTAKER, null, null, any(), any(), null) }
            verify { fagsak.leggTilBehandling(nyBehandling) }
            verify { mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(99L, any(), any(), any()) }
            verify { oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(nyBehandling, any(), any(), any(), any()) }
            verify { skjemaSakMappingService.lagreMapping(any(), any(), any(), any(), any()) }
            prosessinstans.behandling shouldBe nyBehandling
        }
    }

    // --- Helpers ---

    private fun lagBehandling(
        status: Behandlingsstatus,
        behandlingstype: Behandlingstyper = Behandlingstyper.FØRSTEGANG
    ): Behandling {
        return Behandling.forTest {
            id = this@HåndterEksisterendeSakDigitalSøknadTest.behandlingId
            this.status = status
            type = behandlingstype
            this.fagsak = Fagsak.forTest { this.saksnummer = this@HåndterEksisterendeSakDigitalSøknadTest.saksnummer }
        }
    }

    private fun lagFagsakMedBehandling(behandling: Behandling): Fagsak = behandling.fagsak

    private fun lagFagsakUtenAktivBehandling(): Fagsak {
        val fagsak = mockk<Fagsak>(relaxed = true)
        every { fagsak.saksnummer } returns saksnummer
        every { fagsak.finnAktivBehandlingIkkeÅrsavregning() } returns null
        every { fagsak.finnBrukersAktørID() } returns "1234567890123"
        every { fagsak.finnVirksomhetsOrgnr() } returns "123456789"
        return fagsak
    }

    private fun lagProsessinstans(): Prosessinstans = Prosessinstans.forTest {
        medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
        medData(ProsessDataKey.SAKSNUMMER, saksnummer)
    }

    private fun mockFagsakService(fagsak: Fagsak) {
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
    }

    private fun mockEndreStatus() {
        every { behandlingService.endreStatus(any<Behandling>(), any()) } just Runs
    }

    private fun mockTømBehandlingsresultat() {
        every { behandlingsresultatService.tømBehandlingsresultat(any()) } just Runs
    }

    private fun mockOppdaterMottatteOpplysninger() {
        every { mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(any(), any(), any()) } just Runs
    }

    private fun mockHentMottatteOpplysninger(behandlingId: Long) {
        every { mottatteOpplysningerService.hentMottatteOpplysninger(behandlingId) } returns
            mockk<MottatteOpplysninger> { every { id } returns mottatteOpplysningerId }
    }
}
