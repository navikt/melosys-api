package no.nav.melosys.service.oppgave

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.integrasjon.joark.JoarkFasade
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class OppgaveSoekFilterTest {
    @MockK
    private lateinit var oppgaveFasade: OppgaveFasade

    @MockK
    private lateinit var joarkFasade: JoarkFasade

    @MockK
    private lateinit var persondataFasade: PersondataFasade

    private lateinit var oppgaveSoekFilter: OppgaveSoekFilter

    @BeforeEach
    fun setUp() {
        oppgaveSoekFilter = OppgaveSoekFilter(
            oppgaveFasade,
            joarkFasade,
            persondataFasade
        )
        every { joarkFasade.hentMottaksDatoForJournalpost(JOURNALPOST_ID_1) } returns LocalDate.EPOCH
        every { joarkFasade.hentMottaksDatoForJournalpost(JOURNALPOST_ID_2) } returns null
    }

    @Test
    fun `finnBehandlingsoppgaverMedPersonIdent filtrerer oppgaver med journalpost og mottaksdato`() {
        val oppgave1 = Oppgave.Builder().setJournalpostId(JOURNALPOST_ID_1).build()
        val oppgave2 = Oppgave.Builder().setJournalpostId(JOURNALPOST_ID_2).build()
        val oppgave3 = Oppgave.Builder().setJournalpostId(null).build()
        every { persondataFasade.hentAktørIdForIdent(PERSON_IDENT) } returns "aktørID"
        every { oppgaveFasade.finnOppgaverMedAktørId(eq("aktørID"), any()) } returns listOf(oppgave1, oppgave2, oppgave3)


        val oppgaver = oppgaveSoekFilter.finnBehandlingsoppgaverMedPersonIdent(PERSON_IDENT)


        oppgaver.shouldContainExactly(oppgave1)
    }

    @Test
    fun `finnBehandlingsoppgaverMedOrgnr filtrerer oppgaver med journalpost og mottaksdato`() {
        val oppgave1 = Oppgave.Builder().setJournalpostId(JOURNALPOST_ID_1).build()
        val oppgave2 = Oppgave.Builder().setJournalpostId(JOURNALPOST_ID_2).build()
        val oppgave3 = Oppgave.Builder().setJournalpostId(null).build()
        every { oppgaveFasade.finnOppgaverMedOrgnr(eq(ORGNR), any()) } returns listOf(oppgave1, oppgave2, oppgave3)


        val oppgaver = oppgaveSoekFilter.finnBehandlingsoppgaverMedOrgnr(ORGNR)


        oppgaver.shouldContainExactly(oppgave1)
    }

    companion object {
        const val PERSON_IDENT = "fnr"
        const val ORGNR = "986"
        const val JOURNALPOST_ID_1 = "JP_med_dato"
        const val JOURNALPOST_ID_2 = "JP_uten_dato"
    }
}
