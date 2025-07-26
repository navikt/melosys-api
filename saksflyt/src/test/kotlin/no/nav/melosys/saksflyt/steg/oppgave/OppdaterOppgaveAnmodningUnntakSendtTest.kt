package no.nav.melosys.saksflyt.steg.oppgave

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.lagFagsak
import no.nav.melosys.domain.buildWithDefaults
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.*

@ExtendWith(MockKExtension::class)
internal class OppdaterOppgaveAnmodningUnntakSendtTest {
    @MockK
    private lateinit var oppgaveService: OppgaveService

    private val oppgaveSlot = slot<OppgaveOppdatering>()
    private lateinit var oppdaterOppgaveAnmodningUnntakSendt: OppdaterOppgaveAnmodningUnntakSendt
    private lateinit var prosessinstans: Prosessinstans

    @BeforeEach
    fun setUp() {
        oppdaterOppgaveAnmodningUnntakSendt = OppdaterOppgaveAnmodningUnntakSendt(oppgaveService)
        val toMånederFremITid = LocalDate.now().plusMonths(2L)
        val behandling = Behandling.buildWithDefaults {
            dokumentasjonSvarfristDato = Instant.from(ZonedDateTime.of(toMånederFremITid, LocalTime.MAX, ZoneId.systemDefault()))
            fagsak = lagFagsak()
        }
        prosessinstans = Prosessinstans().apply {
            this.behandling = behandling
            setData(ProsessDataKey.OPPGAVE_ID, OPPGAVE_ID)
        }
        every { oppgaveService.oppdaterOppgave(any(), any<OppgaveOppdatering>()) } returns Unit
    }

    @Test
    fun oppdaterOppgaveBeskrivelseOgFrist_eksisterendeFristForKort_fristSettes() {
        val enMånedFremITid = LocalDate.now().plusMonths(1L)
        val oppgave = lagOppgave(enMånedFremITid, null)
        every { oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(any<String>()) } returns oppgave
        val toMånederFremITid = LocalDate.now().plusMonths(2L)

        oppdaterOppgaveAnmodningUnntakSendt.utfør(prosessinstans)

        verify { oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(FagsakTestFactory.SAKSNUMMER) }
        verify { oppgaveService.oppdaterOppgave(eq(oppgave.oppgaveId), capture(oppgaveSlot)) }
        oppgaveSlot.captured.run {
            fristFerdigstillelse.shouldBe(toMånederFremITid)
            beskrivelse.shouldBe(ANMODNING_UNNTAK_BESKRIVELSE)
        }
    }

    @Test
    fun oppdaterOppgaveBeskrivelseOgFrist_eksisterendeFristLangNok_fristBeholdes() {
        val eksisterendeBeskrivelse = "Eksisterende beskrivelse"
        val treMånederFremITid = LocalDate.now().plusMonths(3L)
        val oppgave = lagOppgave(treMånederFremITid, eksisterendeBeskrivelse)
        every { oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(any<String>()) } returns oppgave

        oppdaterOppgaveAnmodningUnntakSendt.utfør(prosessinstans)

        verify { oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(FagsakTestFactory.SAKSNUMMER) }
        verify { oppgaveService.oppdaterOppgave(eq(oppgave.oppgaveId), capture(oppgaveSlot)) }
        oppgaveSlot.captured.run {
            fristFerdigstillelse.shouldBeNull()
            beskrivelse.shouldBe(ANMODNING_UNNTAK_BESKRIVELSE)
        }
    }

    private fun lagOppgave(fristFerdigstillelse: LocalDate, beskrivelse: String?): Oppgave = Oppgave.Builder().apply {
        setFristFerdigstillelse(fristFerdigstillelse)
        setOppgaveId(OPPGAVE_ID)
        setBeskrivelse(beskrivelse)
    }.build()

    companion object {
        private const val OPPGAVE_ID = "123"
        private const val ANMODNING_UNNTAK_BESKRIVELSE = "Anmodning om unntak er sendt utenlandsk trygdemyndighet."
    }
}
