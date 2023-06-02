package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsystem
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.oppgave.PrioritetType
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Component
class OppgaveFactory {
    private val oppgaveBehandlingstemaUtleder = OppgaveBehandlingstemaUtleder()
    private val oppgavetypeUtleder = OppgavetypeNyUtleder()
    private val oppgaveBeskrivelseUtleder = OppgaveBeskrivelseNyUtleder()
    private val temaUtleder = OppgaveTemaUtleder()

    fun lagBehandlingsoppgave(
        behandling: Behandling,
        mottaksdato: LocalDate?,
        hentSedDokument: () -> SedDokument?
    ): Oppgave.Builder {
        // Dokumentasjon for regler: https://confluence.adeo.no/display/TEESSI/Oppgaver+i+Gosys
        val sakstype = behandling.fagsak.type
        val sakstema = behandling.fagsak.tema
        val behandlingstema = behandling.tema
        val behandlingstype = behandling.type
        val oppgaveBehandlingstema = utledOppgaveBehandlingstema(sakstype, sakstema, behandlingstema, behandlingstype)
        return Oppgave.Builder()
            .setBehandlesAvApplikasjon(Fagsystem.MELOSYS)
            .setPrioritet(PrioritetType.NORM)
            .setBehandlingstema(oppgaveBehandlingstema?.kode)
            .setBehandlingstype(null)
            .setTema(utledTema(sakstype, sakstema, behandlingstema))
            .setOppgavetype(utledOppgavetype(sakstype, sakstema, behandlingstema, behandlingstype))
            .setBeskrivelse(
                utledBeskrivelse(
                    oppgaveBehandlingstema,
                    sakstype,
                    sakstema,
                    behandlingstema,
                    behandlingstype
                ) { logHvisMangler ->
                    hentSedDokument().apply {
                        if (logHvisMangler && this == null) log.warn("Sed dokument mangler for:${behandling.fagsak.saksnummer} behandlingID:${behandling.id}")
                    }
                }
            )
            .setFristFerdigstillelse(Behandling.utledBehandlingsfrist(behandling, mottaksdato))
    }

    fun utledOppgaveBehandlingstema(
        sakstype: Sakstyper, sakstema: Sakstemaer, behandlingstema: Behandlingstema, behandlingstype: Behandlingstyper?
    ): OppgaveBehandlingstema? =
        oppgaveBehandlingstemaUtleder.utledOppgaveBehandlingstema(
            sakstype, sakstema, behandlingstema, behandlingstype
        )

    fun utledTema(sakstype: Sakstyper, sakstema: Sakstemaer?, behandlingstema: Behandlingstema): Tema =
        temaUtleder.utledTema(sakstype, sakstema, behandlingstema)

    internal fun utledOppgavetype(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Oppgavetyper {
        return oppgavetypeUtleder.utledOppgavetype(sakstype, sakstema, behandlingstema, behandlingstype)
    }

    internal fun utledBeskrivelse(
        oppgaveBehandlingstema: OppgaveBehandlingstema?,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        hentSedDokument: (logHvisMangler: Boolean) -> SedDokument?
    ): String = oppgaveBeskrivelseUtleder.utledBeskrivelse(
        oppgaveBehandlingstema,
        sakstype,
        sakstema,
        behandlingstema,
        behandlingstype,
        hentSedDokument
    )

    companion object {
        private const val FRIST_FERDIGSTILLELSE_JFR_OPPG: Long = 7

        @JvmStatic
        fun lagJournalføringsoppgave(journalpostID: String): Oppgave.Builder {
            return Oppgave.Builder()
                .setOppgavetype(Oppgavetyper.JFR)
                .setTema(Tema.MED)
                .setPrioritet(PrioritetType.NORM)
                .setJournalpostId(journalpostID)
                .setFristFerdigstillelse(LocalDate.now().plusDays(FRIST_FERDIGSTILLELSE_JFR_OPPG))
        }
    }
}
