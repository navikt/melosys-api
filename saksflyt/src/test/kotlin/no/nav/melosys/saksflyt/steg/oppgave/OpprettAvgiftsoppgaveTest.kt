package no.nav.melosys.saksflyt.steg.oppgave

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Captor

@ExtendWith(MockKExtension::class)
internal class OpprettAvgiftsoppgaveTest {
    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var oppgaveService: OppgaveService

    private val unleash = FakeUnleash()

    @Captor
    private val oppgaveSlot = slot<Oppgave>()
    private lateinit var opprettAvgiftsoppgave: OpprettAvgiftsoppgave
    private lateinit var behandlingsresultat: Behandlingsresultat

    @BeforeEach
    fun setUp() {
        unleash.resetAll()
        opprettAvgiftsoppgave = OpprettAvgiftsoppgave(behandlingsresultatService, oppgaveService, unleash)
        behandlingsresultat = Behandlingsresultat().apply {
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        }
        every { behandlingsresultatService.hentBehandlingsresultat(any<Long>()) } returns behandlingsresultat
    }

    @Test
    fun utfør_togglePå_gjørIngenting() {
        unleash.enable(ToggleName.MELOSYS_IKKE_SEND_TRYGDEAGIFT_OPPGAVE)

        opprettAvgiftsoppgave.utfør(lagProsessinstans())

        verify(exactly = 0) { oppgaveService.opprettOppgave(any()) }
    }

    @Test
    fun utfør_oppretterRiktigOppgave() {
        every { oppgaveService.opprettOppgave(any<Oppgave>()) } returns ""
        behandlingsresultat.lovvalgsperioder.add(lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2))

        opprettAvgiftsoppgave.utfør(lagProsessinstans())

        verify { oppgaveService.opprettOppgave(capture(oppgaveSlot)) }
        oppgaveSlot.captured.run {
            tema.shouldBe(Tema.TRY)
            oppgavetype.shouldBe(Oppgavetyper.VUR)
            journalpostId.shouldNotBeBlank()
            behandlesAvApplikasjon.shouldBe(Fagsystem.INTET)
            aktørId.shouldBe(FagsakTestFactory.BRUKER_AKTØR_ID)
            beskrivelse.shouldBe(OpprettAvgiftsoppgave.AVGIFTSVURDERING_BESKRIVELSE)
        }
    }

    @Test
    fun utfør_avslag_oppretterIkkeOppgave() {
        val lovvalgsperiode = lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        behandlingsresultat.lovvalgsperioder = setOf(lovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(any<Long>()) } returns behandlingsresultat

        opprettAvgiftsoppgave.utfør(lagProsessinstans())

        verify(exactly = 0) { oppgaveService.opprettOppgave(any()) }
    }

    @Test
    fun utfør_saktypeErftrl_oppretterIkkeOppgave() {
        val lovvalgsperiode = lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1)
        lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        behandlingsresultat.lovvalgsperioder = setOf(lovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(any<Long>()) } returns behandlingsresultat
        val prosessinstans = Prosessinstans()
        val fagsak = lagFagsak(Sakstyper.FTRL)
        prosessinstans.behandling = lagBehandling(fagsak)

        opprettAvgiftsoppgave.utfør(prosessinstans)

        verify(exactly = 0) { oppgaveService.opprettOppgave(any()) }
    }

    @Test
    fun utfør_art13_oppretterIkkeOppgave() {
        behandlingsresultat.lovvalgsperioder.add(lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A))

        opprettAvgiftsoppgave.utfør(lagProsessinstans())

        verify(exactly = 0) { oppgaveService.opprettOppgave(any()) }
    }

    private fun lagProsessinstans(): Prosessinstans =
        Prosessinstans().apply {
            behandling = lagBehandling(lagFagsak(Sakstyper.EU_EOS))
        }

    private fun lagFagsak(sakstype: Sakstyper): Fagsak =
        FagsakTestFactory.builder().type(sakstype).medBruker().build()

    private fun lagBehandling(fagsak: Fagsak): Behandling =
        Behandling().apply {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            this.fagsak = fagsak
            initierendeJournalpostId = "JOURNALPOSTID"
        }

    private fun lagLovvalgsperiode(lovvalgBestemmelse: LovvalgBestemmelse): Lovvalgsperiode =
        Lovvalgsperiode().apply {
            bestemmelse = lovvalgBestemmelse
            lovvalgsland = Land_iso2.NO
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
}
