package no.nav.melosys.service.oppgave

import no.finn.unleash.Unleash
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsystem
import no.nav.melosys.domain.Tema
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.oppgave.Oppgave
import no.nav.melosys.domain.oppgave.PrioritetType
import no.nav.melosys.featuretoggle.ToggleName
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class OppgaveFactory(private val unleash: Unleash) {
    private val oppgaveBehandlingstemaUtleder = OppgaveBehandlingstemUnleashAwareUtleder(unleash)
    private val oppgavetypeUtleder = OppgavetypeUnleashAwareUtleder(unleash)
    private val oppgaveBeskrivelseUtleder = OppgaveBeskrivelseUnleashAwareUtleder(unleash)

    fun lagBehandlingsoppgave(behandling: Behandling, mottaksdato: LocalDate?): Oppgave.Builder {
        // Dokumentasjon for regler: https://confluence.adeo.no/display/TEESSI/Oppgaver+i+Gosys
        val sakstype = behandling.fagsak.type
        val sakstema = behandling.fagsak.tema
        val behandlingstema = behandling.tema
        val behandlingstype = behandling.type
        val oppgaveBehandlingstema = utledOppgaveBehandlingstema(sakstype, sakstema, behandlingstema, behandlingstype)
        val oppgaveBehandlingstype = utledOppgaveBehandlingstype(sakstype, sakstema, behandlingstema)
        return Oppgave.Builder()
            .setBehandlesAvApplikasjon(Fagsystem.MELOSYS)
            .setPrioritet(PrioritetType.NORM)
            .setBehandlingstema(oppgaveBehandlingstema.kode)
            .setBehandlingstype(oppgaveBehandlingstype?.kode)
            .setTema(utledTema(sakstype, sakstema, behandlingstema))
            .setOppgavetype(utledOppgavetype(sakstype, sakstema, behandlingstema, behandlingstype))
            .setBeskrivelse(
                utledBeskrivelse(
                    oppgaveBehandlingstema,
                    sakstype,
                    sakstema,
                    behandlingstema,
                    behandlingstype
                )
            )
            .setFristFerdigstillelse(Behandling.utledBehandlingsfrist(behandling, mottaksdato))
    }

    fun utledOppgaveBehandlingstema(
        sakstype: Sakstyper, sakstema: Sakstemaer, behandlingstema: Behandlingstema, behandlingstype: Behandlingstyper?
    ): OppgaveBehandlingstema =
        oppgaveBehandlingstemaUtleder.utledOppgaveBehandlingstema(
            sakstype, sakstema, behandlingstema, behandlingstype
        )

    fun utledOppgaveBehandlingstype(
        sakstype: Sakstyper, sakstema: Sakstemaer, behandlingstema: Behandlingstema
    ): OppgaveBehandlingstype? {
        if (brukNyMapping()) return null

        return if (sakstype == Sakstyper.EU_EOS && sakstema == Sakstemaer.MEDLEMSKAP_LOVVALG && behandlingstema == Behandlingstema.BESLUTNING_LOVVALG_NORGE) {
            OppgaveBehandlingstype.EOS_LOVVALG_NORGE
        } else null
    }

    fun utledTema(sakstype: Sakstyper, sakstema: Sakstemaer?, behandlingstema: Behandlingstema): Tema {
        if (brukNyMapping() && sakstype == Sakstyper.FTRL && behandlingstema == Behandlingstema.UNNTAK_MEDLEMSKAP)
            return Tema.UFM

        return when (sakstema) {
            Sakstemaer.MEDLEMSKAP_LOVVALG -> Tema.MED
            Sakstemaer.TRYGDEAVGIFT -> Tema.TRY
            Sakstemaer.UNNTAK -> Tema.UFM
            else -> {
                throw IllegalStateException("ingen mapping for sakstema:$sakstema")
            }
        }
    }

    private fun utledOppgavetype(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Oppgavetyper {
        return oppgavetypeUtleder.utledOppgavetype(sakstype, sakstema, behandlingstema, behandlingstype)
    }

    private fun utledBeskrivelse(
        oppgaveBehandlingstema: OppgaveBehandlingstema,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): String = oppgaveBeskrivelseUtleder.utledBeskrivelse(
        oppgaveBehandlingstema,
        sakstype,
        sakstema,
        behandlingstema,
        behandlingstype
    )

    private fun brukNyMapping() = unleash.isEnabled(ToggleName.NY_GOSYS_MAPPING)

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
