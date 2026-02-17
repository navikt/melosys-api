package no.nav.melosys.saksflyt.steg.soknad

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.sak.OpprettSakRequest
import no.nav.melosys.skjema.types.DegSelvMetadata
import no.nav.melosys.skjema.types.Skjemadel
import no.nav.melosys.skjema.types.UtsendtArbeidstakerSkjemaDto
import no.nav.melosys.skjema.types.arbeidstaker.UtsendtArbeidstakerArbeidstakersSkjemaDataDto
import no.nav.melosys.skjema.types.common.SkjemaStatus
import no.nav.melosys.skjema.types.m2m.UtsendtArbeidstakerM2MSkjemaData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class OpprettSakOgBehandlingSøknadTest {

    @MockK
    lateinit var fagsakService: FagsakService

    @MockK
    lateinit var persondataFasade: PersondataFasade

    private lateinit var opprettSakOgBehandlingSøknad: OpprettSakOgBehandlingSøknad
    private lateinit var prosessinstans: Prosessinstans

    private val fnr = "12345678901"
    private val aktørId = "1234567890123"
    private val orgnr = "123456789"
    private val juridiskEnhetOrgnr = "987654321"
    private val referanseId = "MEL-TEST123"

    private val søknadsdata = UtsendtArbeidstakerM2MSkjemaData(
        skjemaer = listOf(
            UtsendtArbeidstakerSkjemaDto(
                id = UUID.randomUUID(),
                status = SkjemaStatus.SENDT,
                fnr = fnr,
                orgnr = orgnr,
                metadata = DegSelvMetadata(
                    skjemadel = Skjemadel.ARBEIDSTAKERS_DEL,
                    arbeidsgiverNavn = "Test AS",
                    juridiskEnhetOrgnr = juridiskEnhetOrgnr
                ),
                data = UtsendtArbeidstakerArbeidstakersSkjemaDataDto()
            )
        ),
        referanseId = referanseId
    )

    @BeforeEach
    fun setup() {
        opprettSakOgBehandlingSøknad = OpprettSakOgBehandlingSøknad(fagsakService, persondataFasade)

        prosessinstans = Prosessinstans.forTest {
            medData(ProsessDataKey.SØKNADSDATA, søknadsdata)
        }
    }

    @Test
    fun `inngangsSteg returnerer OPPRETT_SAK_OG_BEHANDLING_SØKNAD`() {
        opprettSakOgBehandlingSøknad.inngangsSteg() shouldBe ProsessSteg.OPPRETT_SAK_OG_BEHANDLING_SØKNAD
    }

    @Test
    fun `utfør oppretter fagsak og behandling med korrekte verdier`() {
        val fagsak = mockk<Fagsak>()
        val behandling = mockk<Behandling>()
        val requestSlot = slot<OpprettSakRequest>()

        every { persondataFasade.hentAktørIdForIdent(fnr) } returns aktørId
        every { fagsakService.nyFagsakOgBehandling(capture(requestSlot)) } returns fagsak
        every { fagsak.hentAktivBehandling() } returns behandling

        opprettSakOgBehandlingSøknad.utfør(prosessinstans)

        verify { persondataFasade.hentAktørIdForIdent(fnr) }
        verify { fagsakService.nyFagsakOgBehandling(any()) }

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
        val fagsak = mockk<Fagsak>()
        val behandling = mockk<Behandling>()

        every { persondataFasade.hentAktørIdForIdent(fnr) } returns aktørId
        every { fagsakService.nyFagsakOgBehandling(any()) } returns fagsak
        every { fagsak.hentAktivBehandling() } returns behandling

        opprettSakOgBehandlingSøknad.utfør(prosessinstans)

        prosessinstans.behandling shouldBe behandling
    }
}
