package no.nav.melosys.service.dokument.brev

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SedSomBrevServiceTest {

    @RelaxedMockK
    lateinit var eessiServiceMock: EessiService

    @RelaxedMockK
    lateinit var joarkFasadeMock: JoarkFasade

    @MockK
    lateinit var persondataFasadeMock: PersondataFasade

    @MockK
    lateinit var utenlandskMyndighetServiceMock: UtenlandskMyndighetService

    lateinit var sedSomBrevService: SedSomBrevService

    private val oppgaveFactory = OppgaveFactory(FakeUnleash())

    @BeforeEach
    internal fun setUp() {
        sedSomBrevService = SedSomBrevService(
            eessiServiceMock,
            joarkFasadeMock,
            persondataFasadeMock,
            utenlandskMyndighetServiceMock,
            oppgaveFactory
        )
    }

    @Test
    fun lagJournalpostForSendingAvSedSomBrevTest() {
        val fagsak: Fagsak = mockk<Fagsak>()
        val behandling = Behandling()
        behandling.id = 123
        behandling.tema = Behandlingstema.YRKESAKTIV
        behandling.status = Behandlingsstatus.UNDER_BEHANDLING
        behandling.fagsak = fagsak

        val utenlandskMyndighet: UtenlandskMyndighet =
            UtenlandskMyndighet().apply { institusjonskode = "X7"; landkode = Land_iso2.AX }

        every { utenlandskMyndighetServiceMock.hentUtenlandskMyndighet(Land_iso2.SE) } returns utenlandskMyndighet
        every { fagsak.hentBrukersAktørID() } returns AKTØR_ID
        every { persondataFasadeMock.hentFolkeregisterident(any()) } returns BRUKER_FNR
        every { fagsak.saksnummer } returns SAKSNUMMER
        every { fagsak.tema } returns Sakstemaer.MEDLEMSKAP_LOVVALG
        every { fagsak.type } returns Sakstyper.EU_EOS


        sedSomBrevService.lagJournalpostForSendingAvSedSomBrev(SedType.A002, Land_iso2.SE, behandling, null)


        verify {
            joarkFasadeMock.opprettJournalpost(withArg {opprettJournalpost ->
                opprettJournalpost.tema.shouldBe(Tema.MED.name)
            }, true)
        }
    }

    companion object {
        val BRUKER_FNR = "12345678922"
        val AKTØR_ID = "551122"
        val SAKSNUMMER= "789456"
    }

}
