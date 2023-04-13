package no.nav.melosys.service.oppgave

import no.finn.unleash.Unleash
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName

class OppgaveBehandlingstemUnleashAwareUtleder(private val unleash: Unleash) : OppgaveBehandlingstemaUtleder {
    private val oppgaveBehandlingstemaGammelUtleder: OppgaveBehandlingstemaUtleder by lazy {
        OppgaveBehandlingstemaGammelUtleder()
    }
    private val oppgaveBehandlingstemaNyUtleder: OppgaveBehandlingstemaUtleder by lazy {
        OppgaveBehandlingstemaNyUtleder()
    }

    override fun utledOppgaveBehandlingstema(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): OppgaveBehandlingstema =
        oppgaveBehandlingstemaUtleder.utledOppgaveBehandlingstema(
            sakstype, sakstema, behandlingstema, behandlingstype
        )

    private val oppgaveBehandlingstemaUtleder: OppgaveBehandlingstemaUtleder
        get() = if (brukNyMapping())
            oppgaveBehandlingstemaNyUtleder
        else
            oppgaveBehandlingstemaGammelUtleder

    private fun brukNyMapping() = unleash.isEnabled(ToggleName.NY_GOSYS_MAPPING)
}
