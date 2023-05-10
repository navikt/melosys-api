package no.nav.melosys.service.oppgave

import no.finn.unleash.Unleash
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgaveBeskrivelseNyUtleder(unleash: Unleash) : OppgaveBeskrivelseUtleder {
    private val oppgaveGosysMapping = OppgaveGosysMapping(unleash)

    override fun utledBeskrivelse(
        oppgaveBehandlingstema: OppgaveBehandlingstema?,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        hentSedDokument: (logVisFeiler: Boolean) -> SedDokument?
    ): String {
        val beskrivelsefelt =
            oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype).beskrivelsefelt

        return when (beskrivelsefelt) {
            OppgaveGosysMapping.Beskrivelsefelt.TOMT -> ""
            OppgaveGosysMapping.Beskrivelsefelt.SED_ELLER_TOMT -> hentSedDokument(false)?.sedType?.name ?: ""
            OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR -> beskrivelsefelt.beskrivelse
            OppgaveGosysMapping.Beskrivelsefelt.SED -> hentSedDokument(true)?.sedType?.name ?: ""
            OppgaveGosysMapping.Beskrivelsefelt.BEHANDLINGSTEMA -> behandlingstema.beskrivelse
        }
    }
}
