package no.nav.melosys.saksflyt.steg.oppgave

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.lagFagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.prosessinstansForTest
import no.nav.melosys.service.oppgave.OppgaveBehandlingstema
import no.nav.melosys.service.oppgave.OppgaveFactory
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class GjenbrukOppgaveTest {
    @MockK
    private lateinit var oppgaveService: OppgaveService

    private val oppgaveSlot = slot<Oppgave>()
    private lateinit var gjenbrukOppgave: GjenbrukOppgave
    private val oppgaveFactory = OppgaveFactory()

    @BeforeEach
    fun setUp() {
        gjenbrukOppgave = GjenbrukOppgave(oppgaveService)
        every { oppgaveService.opprettOppgave(any<Oppgave>()) } returns ""
    }

    @Test
    fun gjenbrukOppgave_utfør_oppdatererOppgave() {
        val oppgaveID = "1234"
        val oppgaveBeskrivelse = "jeg beskriver oppgave"
        val eksisterendeOppgave = Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build()
        every { oppgaveService.hentOppgaveMedOppgaveID(oppgaveID) } returns eksisterendeOppgave
        val prosessinstans = lagProsessinstans(oppgaveID, false)
        every { oppgaveService.lagBehandlingsoppgave(any()) } returns oppgaveFactory.lagBehandlingsoppgave(
            prosessinstans.behandling,
            LocalDate.now(),
            hentSedDokument = { null }
        )

        gjenbrukOppgave.utfør(prosessinstans)

        verify { oppgaveService.opprettOppgave(capture(oppgaveSlot)) }
        oppgaveSlot.captured.run {
            saksnummer.shouldBe(FagsakTestFactory.SAKSNUMMER)
            behandlesAvApplikasjon.shouldBe(Fagsystem.MELOSYS)
            oppgavetype.shouldBe(Oppgavetyper.BEH_SAK_MK)
            behandlingstema.shouldBe(OppgaveBehandlingstema.EU_EOS_YRKESAKTIV.kode)
            behandlingstype.shouldBeNull()
            tilordnetRessurs.shouldBe("Deg321")
            aktørId.shouldBe("123321")
        }
    }

    @Test
    fun gjenbrukOppgave_utfør_oppdatererOppgave_virksomhet() {
        val oppgaveID = "1234"
        val oppgaveBeskrivelse = "jeg beskriver oppgave"
        val eksisterendeOppgave = Oppgave.Builder().setBeskrivelse(oppgaveBeskrivelse).build()
        every { oppgaveService.hentOppgaveMedOppgaveID(oppgaveID) } returns eksisterendeOppgave
        val prosessinstans = lagProsessinstans(oppgaveID, true)
        every { oppgaveService.lagBehandlingsoppgave(any()) } returns oppgaveFactory.lagBehandlingsoppgave(
            prosessinstans.behandling,
            LocalDate.now(),
            hentSedDokument = { null }
        )

        gjenbrukOppgave.utfør(prosessinstans)

        verify { oppgaveService.opprettOppgave(capture(oppgaveSlot)) }
        oppgaveSlot.captured.run {
            saksnummer.shouldBe(FagsakTestFactory.SAKSNUMMER)
            behandlesAvApplikasjon.shouldBe(Fagsystem.MELOSYS)
            oppgavetype.shouldBe(Oppgavetyper.BEH_SAK_MK)
            behandlingstema.shouldBe(OppgaveBehandlingstema.EU_EOS_YRKESAKTIV.kode)
            behandlingstype.shouldBeNull()
            tilordnetRessurs.shouldBe("Deg321")
            orgnr.shouldBe("999999999")
        }
    }

    private fun lagProsessinstans(oppgaveID: String, erForVirksomhet: Boolean): Prosessinstans = prosessinstansForTest {
        behandling(Behandling.forTest {
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            type = Behandlingstyper.FØRSTEGANG
            fagsak = lagFagsak().apply {
                if (erForVirksomhet) {
                    leggTilAktør(Aktoer().apply {
                        orgnr = "999999999"
                        rolle = Aktoersroller.VIRKSOMHET
                    })
                } else {
                    leggTilAktør(Aktoer().apply {
                        aktørId = "123321"
                        rolle = Aktoersroller.BRUKER
                    })
                }
            }
        })
        data(ProsessDataKey.OPPGAVE_ID, oppgaveID)
        data(ProsessDataKey.SKAL_TILORDNES, true)
        data(ProsessDataKey.SAKSBEHANDLER, "Deg321")
    }
}
