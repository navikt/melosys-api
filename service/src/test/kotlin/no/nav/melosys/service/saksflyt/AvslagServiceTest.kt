package no.nav.melosys.service.saksflyt

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AvslagServiceTest {
    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var dokgenService: DokgenService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    @MockK
    private lateinit var fagsakService: FagsakService

    private val slotBehandlingsresultat = slot<Behandlingsresultat>()
    private val slotBrevbestillingDto = slot<BrevbestillingDto>()

    private lateinit var avslagService: AvslagService

    @BeforeEach
    fun setup() {
        slotBehandlingsresultat.clear()
        slotBrevbestillingDto.clear()
        avslagService = AvslagService(
            behandlingService, behandlingsresultatService,
            dokgenService, oppgaveService, fagsakService
        )
    }

    @Test
    fun avslåPgaManglendeOpplysninger_lagrerOgKallerRiktig() {
        val behandling = Behandling().apply {
            fagsak = Fagsak().apply { saksnummer = "saksnummer" }
        }
        every { behandlingService.hentBehandling(1L) }.returns(behandling)
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(Behandlingsresultat())
        every { behandlingsresultatService.lagre(any()) }.returns(Unit)
        every { dokgenService.produserOgDistribuerBrev(any(), any()) }.returns(Unit)
        every { fagsakService.avsluttFagsakOgBehandling(any(), any()) }.returns(Unit)
        every { oppgaveService.ferdigstillOppgaveMedSaksnummer(any()) }.returns(Unit)


        avslagService.avslåPgaManglendeOpplysninger(1L, "fritekst", "Z123456")


        verify(exactly = 1) { behandlingsresultatService.lagre(capture(slotBehandlingsresultat)) }
        verify(exactly = 1) { dokgenService.produserOgDistribuerBrev(eq(1L), capture(slotBrevbestillingDto)) }
        verify(exactly = 1) { fagsakService.avsluttFagsakOgBehandling(behandling.fagsak, Saksstatuser.LOVVALG_AVKLART) }
        verify(exactly = 1) { oppgaveService.ferdigstillOppgaveMedSaksnummer(behandling.fagsak.saksnummer) }

        slotBehandlingsresultat.captured.run {
            fastsattAvLand.shouldBe(Land_iso2.NO)
            type.shouldBe(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL)
            vedtakMetadata.vedtakKlagefrist.shouldNotBeNull()
            vedtakMetadata.vedtakstype.shouldBeNull()
        }
        slotBrevbestillingDto.captured.run {
            produserbardokument.shouldBe(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER)
            mottaker.shouldBe(Mottakerroller.BRUKER)
            bestillersId.shouldBe("Z123456")
            fritekst.shouldBe("fritekst")
        }
    }
}
