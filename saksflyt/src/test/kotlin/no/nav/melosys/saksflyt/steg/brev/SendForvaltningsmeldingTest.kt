package no.nav.melosys.saksflyt.steg.brev

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker
import no.nav.melosys.domain.kodeverk.Mottakerroller.*
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD
import no.nav.melosys.saksflyt.brev.BrevBestiller
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.saksflytapi.skjema.lagUtsendtArbeidstakerSkjemaM2MDto
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.skjema.types.utsendtarbeidstaker.Skjemadel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SendForvaltningsmeldingTest {

    private lateinit var brevBestiller: BrevBestiller
    private lateinit var behandlingService: BehandlingService
    private lateinit var sendForvaltningsmelding: SendForvaltningsmelding

    @BeforeEach
    fun setUp() {
        brevBestiller = mockk(relaxed = true)
        behandlingService = mockk(relaxed = true)
        sendForvaltningsmelding = SendForvaltningsmelding(brevBestiller, behandlingService)
    }

    @Test
    fun `utfør skal bestille forvaltningsmelding når mottaker er bruker`() {
        val behandlingID = 21432L
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingID
            }
            medData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, ForvaltningsmeldingMottaker.BRUKER)
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns prosessinstans.behandling

        sendForvaltningsmelding.utfør(prosessinstans)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) }
        verify {
            brevBestiller.bestill(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                listOf(Mottaker.medRolle(BRUKER)),
                null,
                "TEST",
                null,
                prosessinstans.behandling
            )
        }
    }

    @Test
    fun `utfør skal bestille forvaltningsmelding når mottaker er avsender og avsender er annen person`() {
        val behandlingID = 21432L
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingID
            }
            medData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, ForvaltningsmeldingMottaker.AVSENDER)
            medData(ProsessDataKey.AVSENDER_ID, ANNEN_PERSON_IDENT)
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }


        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns prosessinstans.behandling



        sendForvaltningsmelding.utfør(prosessinstans)


        val forventetMottaker = Mottaker.medRolle(ANNEN_PERSON).apply {
            personIdent = ANNEN_PERSON_IDENT
        }
        verify { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) }
        verify {
            brevBestiller.bestill(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                listOf(forventetMottaker),
                null,
                "TEST",
                null,
                prosessinstans.behandling
            )
        }
    }

    @Test
    fun `utfør skal bestille forvaltningsmelding når mottaker er avsender og avsender er annen organisasjon`() {
        val behandlingID = 21432L
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingID
            }
            medData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, ForvaltningsmeldingMottaker.AVSENDER)
            medData(ProsessDataKey.AVSENDER_ID, ANNEN_ORG_NR)
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns prosessinstans.behandling

        sendForvaltningsmelding.utfør(prosessinstans)

        val forventetMottaker = Mottaker.medRolle(ANNEN_ORGANISASJON).apply {
            orgnr = ANNEN_ORG_NR
        }
        verify { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) }
        verify { brevBestiller.bestill(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, listOf(forventetMottaker), null, "TEST", null, prosessinstans.behandling) }
    }

    @Test
    fun `utfør skal ikke sende forvaltningsmelding når det ikke skal sendes`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            behandling { }
            medData(ProsessDataKey.FORVALTNINGSMELDING_MOTTAKER, ForvaltningsmeldingMottaker.INGEN)
        }


        sendForvaltningsmelding.utfør(prosessinstans)


        verify(exactly = 0) { brevBestiller.bestill(any()) }
    }

    @Test
    fun `utfør skal sende forvaltningsmelding til bruker ved komplett digital søknad i nytt mottak`() {
        val behandlingID = 21432L
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
        }
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingID
            }
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns prosessinstans.behandling

        sendForvaltningsmelding.utfør(prosessinstans)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) }
        verify {
            brevBestiller.bestill(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                listOf(Mottaker.medRolle(BRUKER)),
                null,
                "TEST",
                null,
                prosessinstans.behandling
            )
        }
    }

    @Test
    fun `utfør skal sende forvaltningsmelding til bruker når kun arbeidstakerdel i nytt mottak`() {
        val behandlingID = 21432L
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
        }
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingID
            }
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns prosessinstans.behandling

        sendForvaltningsmelding.utfør(prosessinstans)

        verify { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) }
        verify {
            brevBestiller.bestill(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                listOf(Mottaker.medRolle(BRUKER)),
                null,
                "TEST",
                null,
                prosessinstans.behandling
            )
        }
    }

    @Test
    fun `utfør skal ikke sende forvaltningsmelding når kun arbeidsgiverdel i nytt mottak`() {
        val behandlingID = 21432L
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
        }
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingID
            }
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }

        sendForvaltningsmelding.utfør(prosessinstans)

        verify(exactly = 0) { brevBestiller.bestill(any()) }
        verify(exactly = 0) { behandlingService.hentBehandlingMedSaksopplysninger(any()) }
    }

    @Test
    fun `utfør skal sende forvaltningsmelding til bruker ved komplett digital søknad på eksisterende sak`() {
        val behandlingID = 21432L
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSGIVER_OG_ARBEIDSTAKERS_DEL
        }
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingID
            }
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns prosessinstans.behandling

        sendForvaltningsmelding.utfør(prosessinstans)

        verify {
            brevBestiller.bestill(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                listOf(Mottaker.medRolle(BRUKER)),
                null,
                "TEST",
                null,
                prosessinstans.behandling
            )
        }
    }

    @Test
    fun `utfør skal sende forvaltningsmelding til bruker når kun arbeidstakerdel på eksisterende sak`() {
        val behandlingID = 21432L
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSTAKERS_DEL
        }
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingID
            }
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns prosessinstans.behandling

        sendForvaltningsmelding.utfør(prosessinstans)

        verify {
            brevBestiller.bestill(
                MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD,
                listOf(Mottaker.medRolle(BRUKER)),
                null,
                "TEST",
                null,
                prosessinstans.behandling
            )
        }
    }

    @Test
    fun `utfør skal ikke sende forvaltningsmelding når kun arbeidsgiverdel på eksisterende sak`() {
        val behandlingID = 21432L
        val søknadsdata = lagUtsendtArbeidstakerSkjemaM2MDto {
            skjemadel = Skjemadel.ARBEIDSGIVERS_DEL
            data = no.nav.melosys.skjema.types.utsendtarbeidstaker.UtsendtArbeidstakerArbeidsgiversSkjemaDataDto()
        }
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.MELOSYS_MOTTAK_EKSISTERENDE_DIGITAL_SØKNAD
            status = ProsessStatus.KLAR
            behandling {
                id = behandlingID
            }
            medData(ProsessDataKey.DIGITAL_SØKNADSDATA, søknadsdata)
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }

        sendForvaltningsmelding.utfør(prosessinstans)

        verify(exactly = 0) { brevBestiller.bestill(any()) }
        verify(exactly = 0) { behandlingService.hentBehandlingMedSaksopplysninger(any()) }
    }

    @Test
    fun `utfør skal kaste exception når digital søknadsdata mangler for digital søknad-prosess`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.MELOSYS_MOTTAK_DIGITAL_SØKNAD
            status = ProsessStatus.KLAR
            behandling {
                id = 21432L
            }
            medData(ProsessDataKey.SAKSBEHANDLER, "TEST")
        }

        shouldThrow<IllegalStateException> {
            sendForvaltningsmelding.utfør(prosessinstans)
        }

        verify(exactly = 0) { brevBestiller.bestill(any()) }
    }

    companion object {
        private const val ANNEN_PERSON_IDENT = "21075114491"
        private const val ANNEN_ORG_NR = "999999999"
    }
}
