package no.nav.melosys.service.oppgave

import no.finn.unleash.Unleash
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName

class OppgaveBeskrivelseUnleashAwareUtleder(private val unleash: Unleash) : OppgaveBeskrivelseUtleder {
    private val oppgaveBeskrivelseGammelUtleder: OppgaveBeskrivelseGammelUtleder by lazy {
        OppgaveBeskrivelseGammelUtleder()
    }
    private val oppgaveBeskrivelseNyUtleder: OppgaveBeskrivelseNyUtleder by lazy {
        OppgaveBeskrivelseNyUtleder()
    }

    override fun utledBeskrivelse(
        oppgaveBehandlingstema: OppgaveBehandlingstema,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper,
        sedType: SedType?
    ): String =
        oppgaveBeskrivelseUtleder.utledBeskrivelse(
            oppgaveBehandlingstema,
            sakstype,
            sakstema,
            behandlingstema,
            behandlingstype,
            sedType
        )


    private val oppgaveBeskrivelseUtleder: OppgaveBeskrivelseUtleder
        get() = if (brukNyMapping())
            oppgaveBeskrivelseNyUtleder
        else
            oppgaveBeskrivelseGammelUtleder

    private fun brukNyMapping() = unleash.isEnabled(ToggleName.NY_GOSYS_MAPPING)
}
