package no.nav.melosys.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.Behandlingsnotat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.BehandlingsnotatRepository
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class BehandlingsnotatServiceKtTest {

    @RelaxedMockK
    lateinit var fagsakService: FagsakService

    @RelaxedMockK
    lateinit var behandlingsnotatRepository: BehandlingsnotatRepository

    private lateinit var behandlingsnotatService: BehandlingsnotatService
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun setup() {
        behandlingsnotatService = BehandlingsnotatService(behandlingsnotatRepository, fagsakService)
        fagsak = FagsakTestFactory.lagFagsak()
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    @Test
    fun `opprett notat fagsak har ikke aktiv behandling forvent exception`() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET)

        val exception = shouldThrow<FunksjonellException> {
            behandlingsnotatService.opprettNotat(SAKSNUMMER, "heihei")
        }
        exception.message shouldContain "har ingen aktive behandlinger"
    }

    @Test
    fun `opprett notat fagsak har aktiv behandling blir lagret`() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        val behandling = lagBehandling(fagsak, Behandlingsstatus.ANMODNING_UNNTAK_SENDT)
        every { behandlingsnotatRepository.save(any<Behandlingsnotat>()) } answers { firstArg<Behandlingsnotat>() }

        val tekst = "heiheihei"
        behandlingsnotatService.opprettNotat(SAKSNUMMER, tekst)
        verify { behandlingsnotatRepository.save(any<Behandlingsnotat>()) }
    }

    @Test
    fun `hent notater for fagsak en behandling er avsluttet verifiser redigerbare og ikke redigerbare notater`() {
        every { fagsakService.hentFagsak(any()) } returns fagsak
        val avsluttetBehandling = lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET)
        val ikkeAktivBehandlingsnotat = Behandlingsnotat().apply {
            tekst = "tetetetekksttt"
            setBehandling(avsluttetBehandling)
        }
        avsluttetBehandling.behandlingsnotater.add(ikkeAktivBehandlingsnotat)

        val aktivBehandling = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING)
        val aktivBehandlingsnotat = Behandlingsnotat().apply {
            tekst = "tkkkkkkk"
            setBehandling(aktivBehandling)
        }
        aktivBehandling.behandlingsnotater.add(aktivBehandlingsnotat)

        ikkeAktivBehandlingsnotat.erRedigerbar() shouldBe false
        aktivBehandling.erRedigerbar() shouldBe true

        val notater = behandlingsnotatService.hentNotatForFagsak(SAKSNUMMER)
        notater shouldContainExactlyInAnyOrder listOf(ikkeAktivBehandlingsnotat, aktivBehandlingsnotat)
    }

    @Test
    fun `oppdater notat behandling ikke redigerbar kaster exception`() {
        val notatID = 111L
        val behandling = lagBehandling(fagsak, Behandlingsstatus.AVSLUTTET)
        val behandlingsnotat = Behandlingsnotat().apply {
            id = notatID
            setBehandling(behandling)
            registrertAv = "Z"
        }

        every { behandlingsnotatRepository.findById(notatID) } returns Optional.of(behandlingsnotat)

        val exception = shouldThrow<FunksjonellException> {
            behandlingsnotatService.oppdaterNotat(notatID, "Et skummelt notat.")
        }
        exception.message shouldContain " kan ikke oppdateres, da den tilhører en behandling som er avsluttet"
    }

    @Test
    fun `oppdater notat behandling saksbehandler ikke tilgang kaster exception`() {
        val notatID = 111L
        val behandling = lagBehandling(fagsak, Behandlingsstatus.UNDER_BEHANDLING)
        val behandlingsnotat = Behandlingsnotat().apply {
            id = notatID
            setBehandling(behandling)
            registrertAv = "Z-ukjent"
        }

        every { behandlingsnotatRepository.findById(notatID) } returns Optional.of(behandlingsnotat)

        val exception = shouldThrow<FunksjonellException> {
            behandlingsnotatService.oppdaterNotat(notatID, "Et enda skumlere notat.")
        }
        exception.message shouldContain "Et notat kan ikke endres av andre!"
    }

    private fun lagBehandling(fagsak: Fagsak, behandlingsstatus: Behandlingsstatus) =
        BehandlingTestFactory.builderWithDefaults()
            .medFagsak(fagsak)
            .medStatus(behandlingsstatus)
            .build()
            .also { fagsak.leggTilBehandling(it) }

    companion object {
        private const val SAKSNUMMER = "MEL-123"
    }
}
