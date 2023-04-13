package no.nav.melosys.service.oppgave

import no.finn.unleash.Unleash
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName

class OppgavetypeUnleashAwareUtleder(private val unleash: Unleash) : OppgavetypeUtleder {
    private val oppgavetypeGammelUtleder: OppgavetypeUtleder by lazy {
        OppgavetypeGammelUtleder()
    }
    private val oppgavetypeNyUtleder: OppgavetypeUtleder by lazy {
        OppgavetypeNyUtleder()
    }

    override fun utledOppgavetype(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Oppgavetyper = oppgavetypeUtleder.utledOppgavetype(sakstype, behandlingstema, behandlingstype)

    private val oppgavetypeUtleder: OppgavetypeUtleder
        get() = if (brukNyMapping())
            oppgavetypeNyUtleder
        else
            oppgavetypeGammelUtleder

    private fun brukNyMapping() = unleash.isEnabled(ToggleName.NY_GOSYS_MAPPING)
}
