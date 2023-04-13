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
    private val oppgaveBehandlingstemaFactory = OppgaveBehandlingstemUnleashAwareFactory(unleash)
    private val oppgavetypeFactory = OppgavetypeUnleashAwareFactory(unleash)

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
            .setOppgavetype(utledOppgavetype(sakstype, behandlingstema, behandlingstype))
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
        oppgaveBehandlingstemaFactory.utledOppgaveBehandlingstema(
            sakstype, sakstema, behandlingstema, behandlingstype
        )

    fun utledOppgaveBehandlingstype(
        sakstype: Sakstyper, sakstema: Sakstemaer,
        behandlingstema: Behandlingstema
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
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Oppgavetyper {
        return oppgavetypeFactory.utledOppgavetype(sakstype, behandlingstema, behandlingstype)
    }
    private fun brukNyMapping() = unleash.isEnabled(ToggleName.NY_GOSYS_MAPPING)

    private fun utledBeskrivelse(
        oppgaveBehandlingstema: OppgaveBehandlingstema,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): String {
        return when (oppgaveBehandlingstema) {
            OppgaveBehandlingstema.PENSJONIST_ELLER_UFORETRYGDET -> when (sakstema) {
                Sakstemaer.MEDLEMSKAP_LOVVALG -> sakstype.beskrivelse
                Sakstemaer.TRYGDEAVGIFT -> ""
                Sakstemaer.UNNTAK -> behandlingstema.beskrivelse
            }

            OppgaveBehandlingstema.YRKESAKTIV -> ""
            OppgaveBehandlingstema.ANMODNING_UNNTAK -> when (sakstype) {
                Sakstyper.EU_EOS -> "SEDA001"
                Sakstyper.TRYGDEAVTALE -> ""
                Sakstyper.FTRL -> behandlingstema.beskrivelse
            }

            OppgaveBehandlingstema.REGISTRERING_UNNTAK -> when (behandlingstema) {
                Behandlingstema.BESLUTNING_LOVVALG_ANNET_LAND -> "SEDA003"
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING -> "SEDA009"
                Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE -> "SEDA010"
                Behandlingstema.REGISTRERING_UNNTAK -> ""
                else -> behandlingstema.beskrivelse
            }

            OppgaveBehandlingstema.EU_EOS_LAND -> sedEllerDefaultBeskrivelse(
                sakstype,
                behandlingstema,
                behandlingstype,
                "SEDA005"
            )

            OppgaveBehandlingstema.AVTALELAND -> sedEllerDefaultBeskrivelse(
                sakstype,
                behandlingstema,
                behandlingstype,
                "SEDA008"
            )

            else -> behandlingstema.beskrivelse
        }
    }

    private fun sedEllerDefaultBeskrivelse(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        sed: String
    ): String {
        if (sakstype == Sakstyper.EU_EOS && behandlingstype == Behandlingstyper.HENVENDELSE && behandlingstema == Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET)
            return sed

        return behandlingstema.beskrivelse
    }

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
