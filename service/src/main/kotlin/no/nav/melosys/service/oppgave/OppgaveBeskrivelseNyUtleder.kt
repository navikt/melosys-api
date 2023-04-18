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

        if (beskrivelsefelt == OppgaveGosysMapping.Beskrivelsefelt.SED && sedType == null) {
            throw TekniskException("SedType fra behandling er null når beskrivelsefelt er SED")
        }

        return sedType?.name ?: beskrivelsefelt.beskrivelse
    }
}
