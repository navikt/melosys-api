package no.nav.melosys.saksflyt.steg.oppgave

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.lagFagsak
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.oppgave.OppgaveService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.*

@ExtendWith(MockitoExtension::class)
internal class OppdaterOppgaveAnmodningUnntakSendtTest {
    @Mock
    private val oppgaveService: OppgaveService? = null

    @Captor
    private val oppgaveCaptor: ArgumentCaptor<OppgaveOppdatering>? = null
    private var oppdaterOppgaveAnmodningUnntakSendt: OppdaterOppgaveAnmodningUnntakSendt? = null
    private val prosessinstans = Prosessinstans()
    @BeforeEach
    fun setUp() {
        oppdaterOppgaveAnmodningUnntakSendt = OppdaterOppgaveAnmodningUnntakSendt(oppgaveService!!)
        val behandling = Behandling()
        val toMånederFremITid = LocalDate.now().plusMonths(2L)
        behandling.dokumentasjonSvarfristDato =
            Instant.from(ZonedDateTime.of(toMånederFremITid, LocalTime.MAX, ZoneId.systemDefault()))
        behandling.fagsak = lagFagsak()
        prosessinstans.behandling = behandling
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, OPPGAVE_ID)
    }

    @Test
    @Throws(Exception::class)
    fun oppdaterOppgaveBeskrivelseOgFrist_eksisterendeFristForKort_fristSettes() {
        val enMånedFremITid = LocalDate.now().plusMonths(1L)
        val oppgave = lagOppgave(enMånedFremITid, null)
        Mockito.`when`(oppgaveService!!.hentÅpenBehandlingsoppgaveMedFagsaksnummer(ArgumentMatchers.anyString()))
            .thenReturn(oppgave)
        val toMånederFremITid = LocalDate.now().plusMonths(2L)
        oppdaterOppgaveAnmodningUnntakSendt!!.utfør(prosessinstans)
        Mockito.verify(oppgaveService).hentÅpenBehandlingsoppgaveMedFagsaksnummer(FagsakTestFactory.SAKSNUMMER)
        Mockito.verify(oppgaveService)
            .oppdaterOppgave(ArgumentMatchers.eq(oppgave.oppgaveId), oppgaveCaptor!!.capture())
        Assertions.assertThat(oppgaveCaptor.value.fristFerdigstillelse).isEqualTo(toMånederFremITid)
        Assertions.assertThat(oppgaveCaptor.value.beskrivelse).isEqualTo(ANMODNING_UNNTAK_BESKRIVELSE)
    }

    @Test
    @Throws(Exception::class)
    fun oppdaterOppgaveBeskrivelseOgFrist_eksisterendeFristLangNok_fristBeholdes() {
        val eksisterendeBeskrivelse = "Eksisterende beskrivelse"
        val treMånederFremITid = LocalDate.now().plusMonths(3L)
        val oppgave = lagOppgave(treMånederFremITid, eksisterendeBeskrivelse)
        Mockito.`when`(oppgaveService!!.hentÅpenBehandlingsoppgaveMedFagsaksnummer(ArgumentMatchers.anyString()))
            .thenReturn(oppgave)
        oppdaterOppgaveAnmodningUnntakSendt!!.utfør(prosessinstans)
        Mockito.verify(oppgaveService).hentÅpenBehandlingsoppgaveMedFagsaksnummer(FagsakTestFactory.SAKSNUMMER)
        Mockito.verify(oppgaveService)
            .oppdaterOppgave(ArgumentMatchers.eq(oppgave.oppgaveId), oppgaveCaptor!!.capture())
        Assertions.assertThat(oppgaveCaptor.value.fristFerdigstillelse).isNull()
        Assertions.assertThat(oppgaveCaptor.value.beskrivelse).isEqualTo(ANMODNING_UNNTAK_BESKRIVELSE)
    }

    private fun lagOppgave(fristFerdigstillelse: LocalDate, beskrivelse: String?): Oppgave {
        val oppgaveBuilder = Oppgave.Builder()
        oppgaveBuilder.setFristFerdigstillelse(fristFerdigstillelse)
        oppgaveBuilder.setOppgaveId(OPPGAVE_ID)
        oppgaveBuilder.setBeskrivelse(beskrivelse)
        return oppgaveBuilder.build()
    }

    companion object {
        private const val OPPGAVE_ID = "123"
        private const val ANMODNING_UNNTAK_BESKRIVELSE = "Anmodning om unntak er sendt utenlandsk trygdemyndighet."
    }
}
