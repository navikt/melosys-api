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
import no.nav.melosys.skjema.types.utsendtarbeidstaker.ArbeidsgiverensVirksomhetINorgeDto
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.OpprettSakRequest
import no.nav.melosys.service.sak.SkjemaSakMappingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class OpprettSakOgBehandlingDigitalSøknadTest {

    @MockK lateinit var fagsakService: FagsakService
    @MockK lateinit var persondataFasade: PersondataFasade
    @MockK lateinit var mottatteOpplysningerService: MottatteOpplysningerService
    @MockK lateinit var jsonMapper: JsonMapper
    @MockK lateinit var skjemaSakMappingService: SkjemaSakMappingService
    @MockK lateinit var behandlingService: BehandlingService

    private lateinit var opprettSakOgBehandlingDigitalSøknad: OpprettSakOgBehandlingDigitalSøknad
    private lateinit var prosessinstans: Prosessinstans

    private val fnr = "12345678901"
    private val aktørId = "1234567890123"
    private val orgnr = "123456789"
    private val juridiskEnhetOrgnr = "987654321"
    private val referanseId = "MEL-TEST123"
    private val behandlingId = 42L

    private val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
        fnr = this@OpprettSakOgBehandlingDigitalSøknadTest.fnr
        orgnr = this@OpprettSakOgBehandlingDigitalSøknadTest.orgnr
        juridiskEnhetOrgnr = this@OpprettSakOgBehandlingDigitalSøknadTest.juridiskEnhetOrgnr
        referanseId = this@OpprettSakOgBehandlingDigitalSøknadTest.referanseId
    }

    @BeforeEach
    fun setup() {
        opprettSakOgBehandlingDigitalSøknad = OpprettSakOgBehandlingDigitalSøknad(
            fagsakService, persondataFasade, mottatteOpplysningerService, jsonMapper,
            skjemaSakMappingService, behandlingService
        )

        prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
        }

        every { skjemaSakMappingService.lagreMapping(any(), any(), any(), any(), any()) } just Runs
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
        } returns mockk<MottatteOpplysninger> { every { id } returns 99L }
    }

    @Test
    fun `inngangsSteg returnerer OPPRETT_SAK_OG_BEHANDLING_SØKNAD`() {
        opprettSakOgBehandlingDigitalSøknad.inngangsSteg() shouldBe ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_DIGITAL_SØKNAD
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

        opprettSakOgBehandlingDigitalSøknad.utfør(prosessinstans)

        val capturedRequest = requestSlot.captured
        capturedRequest.aktørID shouldBe aktørId
        capturedRequest.sakstype shouldBe Sakstyper.EU_EOS
        capturedRequest.sakstema shouldBe Sakstemaer.MEDLEMSKAP_LOVVALG
        capturedRequest.behandlingstema shouldBe Behandlingstema.UTSENDT_ARBEIDSTAKER
        capturedRequest.behandlingstype shouldBe Behandlingstyper.FØRSTEGANG
        capturedRequest.behandlingsårsaktype shouldBe Behandlingsaarsaktyper.SØKNAD
    }

    @Test
    fun `utfør setter behandlingstema ARBEID_TJENESTEPERSON_ELLER_FLY for offentlig virksomhet`() {
        val offentligSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto(
                arbeidsgiverensVirksomhetINorge = ArbeidsgiverensVirksomhetINorgeDto(
                    erArbeidsgiverenOffentligVirksomhet = true
                )
            )
            fnr = this@OpprettSakOgBehandlingDigitalSøknadTest.fnr
            referanseId = this@OpprettSakOgBehandlingDigitalSøknadTest.referanseId
        }
        val offentligProsessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, offentligSøknadsdata)
        }

        val requestSlot = slot<OpprettSakRequest>()
        every { persondataFasade.hentAktørIdForIdent(fnr) } returns aktørId
        every { fagsakService.nyFagsakOgBehandling(capture(requestSlot)) } returns mockk<Fagsak>().also {
            every { it.hentAktivBehandling() } returns mockk<Behandling>(relaxed = true).also { b ->
                every { b.id } returns behandlingId
                every { b.status } returns Behandlingsstatus.OPPRETTET
            }
            every { it.saksnummer } returns "MEL-1234"
        }
        every { jsonMapper.writeValueAsString(offentligSøknadsdata) } returns "{}"
        every { mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(any(), any(), any(), any()) } returns
            mockk<MottatteOpplysninger> { every { id } returns 99L }

        opprettSakOgBehandlingDigitalSøknad.utfør(offentligProsessinstans)

        requestSlot.captured.behandlingstema shouldBe Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
    }

    @Test
    fun `utfør setter behandling på prosessinstans`() {
        val behandling = mockFagsakOgBehandling()
        mockMottatteOpplysninger()

        opprettSakOgBehandlingDigitalSøknad.utfør(prosessinstans)

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
        } returns mockk<MottatteOpplysninger> { every { id } returns 99L }

        opprettSakOgBehandlingDigitalSøknad.utfør(prosessinstans)

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

        opprettSakOgBehandlingDigitalSøknad.utfør(prosessinstans)

        verify { skjemaSakMappingService.lagreMapping(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `utfør setter AVVENT_DOK_PART når kun arbeidsgiver-del uten koblet motpart`() {
        val arbeidsgiverSøknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
            fnr = this@OpprettSakOgBehandlingDigitalSøknadTest.fnr
            referanseId = this@OpprettSakOgBehandlingDigitalSøknadTest.referanseId
        }
        val arbeidsgiverProsessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, arbeidsgiverSøknadsdata)
        }

        val behandling = mockk<Behandling>(relaxed = true)
        every { behandling.id } returns behandlingId
        every { behandling.status } returns Behandlingsstatus.OPPRETTET
        every { persondataFasade.hentAktørIdForIdent(fnr) } returns aktørId
        every { fagsakService.nyFagsakOgBehandling(any()) } returns mockk<Fagsak>().also {
            every { it.hentAktivBehandling() } returns behandling
            every { it.saksnummer } returns "MEL-1234"
        }
        every { behandlingService.lagre(any()) } just Runs
        every { jsonMapper.writeValueAsString(arbeidsgiverSøknadsdata) } returns "{}"
        every { mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(any(), any(), any(), any()) } returns
            mockk<MottatteOpplysninger> { every { id } returns 99L }

        opprettSakOgBehandlingDigitalSøknad.utfør(arbeidsgiverProsessinstans)

        verify { behandling.status = Behandlingsstatus.AVVENT_DOK_PART }
        verify { behandlingService.lagre(behandling) }
    }

    @Test
    fun `utfør setter IKKE AVVENT_DOK_PART for arbeidstaker-del`() {
        val behandling = mockFagsakOgBehandling()
        mockMottatteOpplysninger()

        opprettSakOgBehandlingDigitalSøknad.utfør(prosessinstans)

        verify(exactly = 0) { behandling.status = Behandlingsstatus.AVVENT_DOK_PART }
    }

    @Test
    fun `utfør setter IKKE AVVENT_DOK_PART for arbeidsgiver-del med koblet motpart`() {
        val arbeidsgiverMedKoblet = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
            fnr = this@OpprettSakOgBehandlingDigitalSøknadTest.fnr
            referanseId = this@OpprettSakOgBehandlingDigitalSøknadTest.referanseId
            medKobletArbeidsgiverSkjema()
        }
        val kobletProsessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, arbeidsgiverMedKoblet)
        }

        val behandling = mockk<Behandling>(relaxed = true)
        every { behandling.id } returns behandlingId
        every { behandling.status } returns Behandlingsstatus.OPPRETTET
        every { persondataFasade.hentAktørIdForIdent(fnr) } returns aktørId
        every { fagsakService.nyFagsakOgBehandling(any()) } returns mockk<Fagsak>().also {
            every { it.hentAktivBehandling() } returns behandling
            every { it.saksnummer } returns "MEL-1234"
        }
        every { jsonMapper.writeValueAsString(arbeidsgiverMedKoblet) } returns "{}"
        every { mottatteOpplysningerService.opprettSøknadUtsendteArbeidstakereEøs(any(), any(), any(), any()) } returns
            mockk<MottatteOpplysninger> { every { id } returns 99L }

        opprettSakOgBehandlingDigitalSøknad.utfør(kobletProsessinstans)

        verify(exactly = 0) { behandlingService.lagre(any()) }
    }
}
