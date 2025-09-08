package no.nav.melosys.saksflyt.steg.behandling

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.domain.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class ReplikerBehandlingTest {

    @MockK
    private lateinit var fagsakService: FagsakService
    @MockK
    private lateinit var behandlingService: BehandlingService
    @MockK
    private lateinit var behandlingReplikeringsRegler: SaksbehandlingRegler

    private lateinit var replikerBehandling: ReplikerBehandling
    private lateinit var fagsak: Fagsak
    private lateinit var prosessinstans: Prosessinstans

    @BeforeEach
    fun setUp() {
        replikerBehandling = ReplikerBehandling(fagsakService, behandlingService, behandlingReplikeringsRegler)

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE)
        }

        fagsak = Fagsak.forTest { }

        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { fagsakService.lagre(any()) } just Runs
    }

    @Test
    fun `utfør skal kaste feil når behandling som er utgangspunkt for vurdering er aktiv`() {
        val behandling = Behandling.forTest {
            id = 1L
            status = Behandlingsstatus.UNDER_BEHANDLING
        }

        val replikertBehandling = Behandling.forTest {
            id = 2L
            status = Behandlingsstatus.UNDER_BEHANDLING
        }

        fagsak.behandlinger.add(behandling)
        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE)
            medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
            medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
        }

        every { behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak) } returns behandling
        every { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE) } returns replikertBehandling


        val exception = assertThrows<FunksjonellException> {
            replikerBehandling.utfør(prosessinstans)
        }


        exception.message shouldBe "Støtter ikke opprettelse av ny behandling når behandling som er utgangspunkt for revurdering er aktiv"
    }

    @Test
    fun `utfør skal sette steg opprett oppgave når finnes behandling som er utgangspunkt for revurdering`() {
        val behandling = Behandling.forTest {
            id = 1L
            status = Behandlingsstatus.AVSLUTTET
        }

        val replikertBehandling = Behandling.forTest {
            id = 2L
            tema = Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND
            type = Behandlingstyper.NY_VURDERING
            fagsak = this@ReplikerBehandlingTest.fagsak
        }

        fagsak.tema = Sakstemaer.MEDLEMSKAP_LOVVALG

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE)
            medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
            medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
        }

        every { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE) } returns replikertBehandling
        every { behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak) } returns behandling


        replikerBehandling.utfør(prosessinstans)


        verify { fagsakService.lagre(fagsak) }
        prosessinstans.behandling shouldBe replikertBehandling
        verify { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE) }
    }

    @Test
    fun `utfør skal sette steg opprett oppgave når finnes ikke behandling som er utgangspunkt for revurdering`() {
        val behandling = Behandling.forTest {
            id = 1L
            status = Behandlingsstatus.AVSLUTTET
        }

        fagsak.behandlinger.add(behandling)

        val replikertBehandling = Behandling.forTest {
            id = 2L
            tema = Behandlingstema.ARBEID_NORGE_BOSATT_ANNET_LAND
            type = Behandlingstyper.NY_VURDERING
            fagsak = this@ReplikerBehandlingTest.fagsak
        }

        fagsak.tema = Sakstemaer.MEDLEMSKAP_LOVVALG

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE)
            medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
            medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
        }

        every { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE) } returns replikertBehandling
        every { behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak) } returns behandling


        replikerBehandling.utfør(prosessinstans)


        verify { fagsakService.lagre(fagsak) }
        prosessinstans.behandling shouldBe replikertBehandling
        verify { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE) }
    }

    @Test
    fun `utfør skal kaste feil når finnes ikke behandling som er utgangspunkt for revurdering`() {
        val behandling = Behandling.forTest {
            id = 1L
            status = Behandlingsstatus.AVSLUTTET
        }

        fagsak.behandlinger.add(behandling)

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE)
            medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
            medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
        }

        every { behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak) } returns null


        val exception = assertThrows<FunksjonellException> {
            replikerBehandling.utfør(prosessinstans)
        }


        exception.message shouldBe "Finner ikke behandling som kan replikeres. Denne fantes ved opprettelse av prosessen"
    }

    @Test
    fun `utfør skal kaste feil når behandlingsårsak er ikke satt`() {
        val behandling = Behandling.forTest {
            id = 1L
            status = Behandlingsstatus.AVSLUTTET
        }

        val replikertBehandling = Behandling.forTest {
            id = 2L
        }

        every { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE) } returns replikertBehandling
        every { behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak) } returns behandling

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE)
            medData(ProsessDataKey.MOTTATT_DATO, LocalDate.now())
        }


        val exception = assertThrows<FunksjonellException> {
            replikerBehandling.utfør(prosessinstans)
        }


        exception.message shouldBe "Mangler mottaksdato eller behandlingsårsaktype"
    }

    @Test
    fun `utfør skal kaste feil når mottaksdato er ikke satt`() {
        val behandling = Behandling.forTest {
            id = 1L
            status = Behandlingsstatus.AVSLUTTET
        }

        val replikertBehandling = Behandling.forTest {
            id = 2L
        }

        every { behandlingService.replikerBehandlingOgBehandlingsresultat(behandling, Behandlingstyper.ENDRET_PERIODE) } returns replikertBehandling
        every { behandlingReplikeringsRegler.finnBehandlingSomKanReplikeres(fagsak) } returns behandling

        prosessinstans = Prosessinstans.forTest {
            type = ProsessType.OPPRETT_SAK
            status = ProsessStatus.KLAR
            medData(ProsessDataKey.SAKSNUMMER, FagsakTestFactory.SAKSNUMMER)
            medData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE)
            medData(ProsessDataKey.BEHANDLINGSÅRSAKTYPE, Behandlingsaarsaktyper.SØKNAD)
        }


        val exception = assertThrows<FunksjonellException> {
            replikerBehandling.utfør(prosessinstans)
        }


        exception.message shouldBe "Mangler mottaksdato eller behandlingsårsaktype"
    }
}
