package no.nav.melosys.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsnotat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingsnotatRepository
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class BehandlingsnotatServiceTest {

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingsnotatRepository: BehandlingsnotatRepository

    private lateinit var behandlingsnotatService: BehandlingsnotatService

    private lateinit var fagsak: Fagsak

    private val saksnummer = "MEL-123"

    @BeforeEach
    fun setup() {
        behandlingsnotatService = BehandlingsnotatService(behandlingsnotatRepository, fagsakService)
        fagsak = Fagsak.forTest { medBruker() }
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    @Test
    fun opprettNotat_fagsakHarIkkeAktivBehandling_forventException() {
        val fagsak = Fagsak.forTest {
            medBruker()
            saksnummer = this@BehandlingsnotatServiceTest.saksnummer
        }
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET)

        val exception = shouldThrow<FunksjonellException> {
            behandlingsnotatService.opprettNotat(saksnummer, "heihei")
        }
        exception.message shouldContain "har ingen aktive behandlinger"
    }

    @Test
    fun opprettNotat_fagsakHarKunAktivAarsavregning_blirLagretPaaAarsavregningen() {
        val fagsak = lagFagsak()
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        val årsavregning = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.ÅRSAVREGNING)
        val captor = slot<Behandlingsnotat>()
        every { behandlingsnotatRepository.save(capture(captor)) } returns Behandlingsnotat()

        behandlingsnotatService.opprettNotat(saksnummer, "heihei")

        captor.captured.behandling shouldBe årsavregning
    }

    @Test
    fun opprettNotat_fagsakHarFlereAktiveAarsavregningerUtenBehandlingId_forventException() {
        val fagsak = lagFagsak()
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.ÅRSAVREGNING)
        lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.ÅRSAVREGNING)

        val exception = shouldThrow<FunksjonellException> {
            behandlingsnotatService.opprettNotat(saksnummer, "heihei")
        }
        exception.message shouldContain "behandlingId må oppgis"
    }

    @Test
    fun opprettNotat_medBehandlingId_blirLagretPaaAngittBehandling() {
        val fagsak = lagFagsak()
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.FØRSTEGANG).apply { id = 1L }
        val årsavregning = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.ÅRSAVREGNING)
            .apply { id = 2L }
        val captor = slot<Behandlingsnotat>()
        every { behandlingsnotatRepository.save(capture(captor)) } returns Behandlingsnotat()

        behandlingsnotatService.opprettNotat(saksnummer, "heihei", 2L)

        captor.captured.behandling shouldBe årsavregning
    }

    @Test
    fun opprettNotat_behandlingIdTilhoererIkkeFagsaken_forventException() {
        val fagsak = lagFagsak()
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.FØRSTEGANG).apply { id = 1L }

        shouldThrow<IkkeFunnetException> {
            behandlingsnotatService.opprettNotat(saksnummer, "heihei", 999L)
        }
    }

    @Test
    fun opprettNotat_behandlingIdPekerPaaAvsluttetBehandling_forventException() {
        val fagsak = lagFagsak()
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET, Behandlingstyper.FØRSTEGANG).apply { id = 1L }

        val exception = shouldThrow<FunksjonellException> {
            behandlingsnotatService.opprettNotat(saksnummer, "heihei", 1L)
        }
        exception.message shouldContain "er ikke aktiv"
    }

    @Test
    fun opprettNotat_baadeAktivBehandlingOgAarsavregningUtenBehandlingId_velgerIkkeAarsavregning() {
        val fagsak = lagFagsak()
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        val ordinær = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.FØRSTEGANG)
        lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING, Behandlingstyper.ÅRSAVREGNING)
        val captor = slot<Behandlingsnotat>()
        every { behandlingsnotatRepository.save(capture(captor)) } returns Behandlingsnotat()

        behandlingsnotatService.opprettNotat(saksnummer, "heihei")

        captor.captured.behandling shouldBe ordinær
    }

    @Test
    fun opprettNotat_fagsakHarAktivBehandling_blirLagret() {
        val fagsak = Fagsak.forTest {
            medBruker()
            saksnummer = this@BehandlingsnotatServiceTest.saksnummer
        }
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        val behandling = lagBehandling(fagsak, Behandlingsstatus.ANMODNING_UNNTAK_SENDT)
        val captor = slot<Behandlingsnotat>()
        every { behandlingsnotatRepository.save(capture(captor)) } returns Behandlingsnotat()

        val tekst = "heiheihei"
        behandlingsnotatService.opprettNotat(saksnummer, tekst)
        verify { behandlingsnotatRepository.save(any()) }

        captor.captured.behandling shouldBe behandling
    }

    @Test
    fun hentNotaterForFagsak_enBehandlingErAvsluttet_verifiserRedigerbareOgIkkeRedigerbareNotater() {
        val fagsak = Fagsak.forTest {
            medBruker()
            saksnummer = this@BehandlingsnotatServiceTest.saksnummer
        }
        every { fagsakService.hentFagsak(saksnummer) } returns fagsak
        val avsluttetBehandling = lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET)
        val ikkeAktivBehandlingsnotat = Behandlingsnotat().apply {
            tekst = "tetetetekksttt"
            this.behandling = avsluttetBehandling
        }
        avsluttetBehandling.behandlingsnotater.add(ikkeAktivBehandlingsnotat)

        val aktivBehandling = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING)
        val aktivBehandlingsnotat = Behandlingsnotat().apply {
            tekst = "tkkkkkkk"
            this.behandling = aktivBehandling
        }
        aktivBehandling.behandlingsnotater.add(aktivBehandlingsnotat)

        ikkeAktivBehandlingsnotat.erRedigerbar() shouldBe false
        aktivBehandling.erRedigerbar() shouldBe true

        val notater = behandlingsnotatService.hentNotatForFagsak(saksnummer)
        notater.shouldContainExactlyInAnyOrder(ikkeAktivBehandlingsnotat, aktivBehandlingsnotat)
    }

    @Test
    fun oppdaterNotat_behandlingIkkeRedigerbar_kasterException() {
        val notatID = 111L
        val behandling = lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET)
        val behandlingsnotat = Behandlingsnotat().apply {
            id = notatID
            this.behandling = behandling
            registrertAv = "Z"
        }

        every { behandlingsnotatRepository.findById(notatID) } returns Optional.of(behandlingsnotat)

        val exception = shouldThrow<FunksjonellException> {
            behandlingsnotatService.oppdaterNotat(notatID, "Et skummelt notat.")
        }
        exception.message shouldContain " kan ikke oppdateres, da den tilhører en behandling som er avsluttet"
    }

    @Test
    fun oppdaterNotat_behandlingSaksbehandlerIkkeTilgang_kasterException() {
        val notatID = 111L
        val behandling = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING)
        val behandlingsnotat = Behandlingsnotat().apply {
            id = notatID
            this.behandling = behandling
            registrertAv = "Z-ukjent"
        }

        every { behandlingsnotatRepository.findById(notatID) } returns Optional.of(behandlingsnotat)

        val exception = shouldThrow<FunksjonellException> {
            behandlingsnotatService.oppdaterNotat(notatID, "Et enda skumlere notat.")
        }
        exception.message shouldContain "Et notat kan ikke endres av andre!"
    }

    private fun lagFagsak(): Fagsak = Fagsak.forTest {
        medBruker()
        saksnummer = this@BehandlingsnotatServiceTest.saksnummer
    }

    private fun lagBehandling(
        fagsak: Fagsak,
        behandlingsstatus: Behandlingsstatus,
        behandlingstype: Behandlingstyper = Behandlingstyper.FØRSTEGANG,
    ): Behandling = Behandling.forTest {
        this.fagsak = fagsak
        status = behandlingsstatus
        type = behandlingstype
    }
}
