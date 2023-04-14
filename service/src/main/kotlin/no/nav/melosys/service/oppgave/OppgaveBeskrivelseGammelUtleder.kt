package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgaveBeskrivelseGammelUtleder : OppgaveBeskrivelseUtleder {

    override fun utledBeskrivelse(
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
}
