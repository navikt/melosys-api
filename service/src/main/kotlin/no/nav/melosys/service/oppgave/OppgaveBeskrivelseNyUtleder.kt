package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.TekniskException

class OppgaveBeskrivelseNyUtleder : OppgaveBeskrivelseUtleder {
    private val oppgaveGosysMapping = OppgaveGosysMapping()

    override fun utledBeskrivelse(
        oppgaveBehandlingstema: OppgaveBehandlingstema,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        sedType: SedType?
    ): String {
        val beskrivelsefelt =
            oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype).beskrivelsefelt

        return when (beskrivelsefelt) {
            OppgaveGosysMapping.Beskrivelsefelt.TOMT -> ""
            OppgaveGosysMapping.Beskrivelsefelt.SED_ELLER_TOMT -> sedType?.name ?: ""
            OppgaveGosysMapping.Beskrivelsefelt.A1_ANMODNING_OM_UNNTAK_PAPIR -> beskrivelsefelt.beskrivelse
            OppgaveGosysMapping.Beskrivelsefelt.SED -> sedType?.name
                ?: throw TekniskException("SedType fra behandling er null når beskrivelsefelt er SED")
        }
    }
}
