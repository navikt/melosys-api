package no.nav.melosys.saksflyt.steg.brev

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.arkiv.Distribusjonstype
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.doksys.DoksysFasade
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.saksflyt.TestdataFactory
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.aktoer.KontaktopplysningService
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class DistribuerJournalpostTest {

    private val mockDoksysFasade: DoksysFasade = mockk()
    private val mockEregFasade: EregFasade = mockk()
    private val mockKontaktopplysningService: KontaktopplysningService = mockk()
    private val mockBehandlingService: BehandlingService = mockk()
    private val mockUtenlandskMyndighetService: UtenlandskMyndighetService = mockk()
    private val mockKodeverkService: KodeverkService = mockk()

    private lateinit var distribuerJournalpost: DistribuerJournalpost

    @BeforeEach
    fun init() {
        distribuerJournalpost = DistribuerJournalpost(
            mockDoksysFasade,
            mockEregFasade,
            mockKontaktopplysningService,
            mockBehandlingService,
            mockUtenlandskMyndighetService,
            mockKodeverkService
        )
        every { mockDoksysFasade.distribuerJournalpost(any<String>(), any<Distribusjonstype>()) } returns "ok"
        every {
            mockDoksysFasade.distribuerJournalpost(
                any<String>(),
                any<StrukturertAdresse>(),
                any(),
                any(),
                any<Distribusjonstype>()
            )
        } returns "ok"
        every { mockDoksysFasade.distribuerJournalpost(any<String>(), any<StrukturertAdresse>(), any<Distribusjonstype>()) } returns "ok"
        every { mockKodeverkService.dekod(any(), any()) } returns "Default"
    }

    @Test
    fun `utfør feiler ved manglende behandling`() {
        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
        }

        shouldThrow<FunksjonellException> {
            distribuerJournalpost.utfør(prosessinstans)
        }
    }

    @Test
    fun `utfør feiler ved manglende journalpost ID`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medBehandling(behandling)
            medData(ProsessDataKey.BREVBESTILLING, DokgenBrevbestilling.Builder<Nothing>().build())
        }

        shouldThrow<FunksjonellException> {
            distribuerJournalpost.utfør(prosessinstans)
        }
    }

    @Test
    fun `utfør feiler ved manglende mottaker`() {
        val behandling = TestdataFactory.lagBehandling()
        every { mockBehandlingService.hentBehandlingMedSaksopplysninger(any()) } returns behandling

        val prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medBehandling(behandling)
            medData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, "123")
            medData(ProsessDataKey.BREVBESTILLING, DokgenBrevbestilling.Builder<Nothing>().build())
        }

        shouldThrow<FunksjonellException> {
            distribuerJournalpost.utfør(prosessinstans)
        }
    }

    @Test
    fun `utfør distribuer journalpost uten adresse`() {
        val journalpostId = "12345"
        val prosessinstans = setupHappypath(journalpostId, Mottakerroller.BRUKER, Distribusjonstype.VIKTIG)


        distribuerJournalpost.utfør(prosessinstans)


        verify { mockDoksysFasade.distribuerJournalpost(journalpostId, Distribusjonstype.VIKTIG) }
    }

    @Test
    fun `utfør distribuer journalpost med postadresse`() {
        val journalpostId = "12345"
        val prosessinstans = setupHappypath(journalpostId, Mottakerroller.FULLMEKTIG, Distribusjonstype.ANNET).apply {
            setData(ProsessDataKey.ORGNR, "123456789")
        }

        val saksopplysning = Saksopplysning().apply {
            dokument = TestdataFactory.lagOrgMedPostadresse()
        }

        every { mockEregFasade.hentOrganisasjon(any()) } returns saksopplysning
        every { mockKontaktopplysningService.hentKontaktopplysning(any(), any()) } returns Optional.of(TestdataFactory.lagKontaktOpplysning())


        distribuerJournalpost.utfør(prosessinstans)


        verify {
            mockDoksysFasade.distribuerJournalpost(
                eq(journalpostId),
                any<StrukturertAdresse>(),
                any(),
                any(),
                eq(Distribusjonstype.ANNET)
            )
        }
    }

    @Test
    fun `utfør distribuer journalpost med forretningsadresse`() {
        val journalpostId = "12345"
        val prosessinstans = setupHappypath(journalpostId, Mottakerroller.FULLMEKTIG, Distribusjonstype.VEDTAK).apply {
            setData(ProsessDataKey.ORGNR, "123456789")
        }

        val saksopplysning = Saksopplysning().apply {
            dokument = TestdataFactory.lagOrgMedForretningsadresse()
        }

        every { mockEregFasade.hentOrganisasjon(any()) } returns saksopplysning
        every { mockKontaktopplysningService.hentKontaktopplysning(any(), any()) } returns Optional.of(TestdataFactory.lagKontaktOpplysning())
        every { mockKodeverkService.dekod(any(), any()) } returns "Andeby"


        distribuerJournalpost.utfør(prosessinstans)


        verify {
            mockDoksysFasade.distribuerJournalpost(
                eq(journalpostId),
                any<StrukturertAdresse>(),
                any(),
                any(),
                eq(Distribusjonstype.VEDTAK)
            )
        }
    }

    @Test
    fun `utfør distribuer journalpost med representant person`() {
        val journalpostId = "12345"
        val prosessinstans = setupHappypath(journalpostId, Mottakerroller.FULLMEKTIG, Distribusjonstype.ANNET).apply {
            setData(ProsessDataKey.AKTØR_ID, "12345678901")
        }


        distribuerJournalpost.utfør(prosessinstans)


        verify { mockDoksysFasade.distribuerJournalpost(journalpostId, Distribusjonstype.ANNET) }
    }

    @Test
    fun `utfør distribuer journalpost med utenlandsk myndighet`() {
        val journalpostId = "12345"
        val institusjonID = "GB:A100"
        val prosessinstans = setupHappypath(journalpostId, Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET, Distribusjonstype.VIKTIG).apply {
            setData(ProsessDataKey.INSTITUSJON_ID, institusjonID)
        }

        val utenlandskMyndighet = UtenlandskMyndighet().apply {
            landkode = Land_iso2.GB
        }

        every { mockUtenlandskMyndighetService.hentUtenlandskMyndighet(eq(Land_iso2.GB), any()) } returns utenlandskMyndighet


        distribuerJournalpost.utfør(prosessinstans)


        verify {
            mockDoksysFasade.distribuerJournalpost(
                eq(journalpostId),
                any<StrukturertAdresse>(),
                eq(Distribusjonstype.VIKTIG)
            )
        }
    }

    private fun setupHappypath(journalpostId: String, rolle: Mottakerroller, distribusjonstype: Distribusjonstype): Prosessinstans {
        val brevbestilling = MangelbrevBrevbestilling.Builder()
            .medDistribusjonstype(distribusjonstype)
            .build()

        every { mockBehandlingService.hentBehandlingMedSaksopplysninger(any()) } returns TestdataFactory.lagBehandling()

        return Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medBehandling(TestdataFactory.lagBehandling())
            medData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, journalpostId)
            medData(ProsessDataKey.BREVBESTILLING, brevbestilling)
            medData(ProsessDataKey.MOTTAKER, rolle)
        }
    }
}
