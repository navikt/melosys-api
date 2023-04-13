package no.nav.melosys.service.oppgave

import no.finn.unleash.Unleash
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName

class OppgaveBehandlingstemUnleashAwareFactory(private val unleash: Unleash) : OppgaveBehandlingstemaFactory {
    private val oppgaveBehandlingstemaFactoryGammelMapping: OppgaveBehandlingstemaFactory by lazy {
        OppgaveBehandlingstemaGammelMappingFactory()
    }
    private val oppgaveBehandlingstemaFactoryNyMapping: OppgaveBehandlingstemaFactory by lazy {
        OppgaveBehandlingstemaNyMappingFactory()
    }

    override fun utledOppgaveBehandlingstema(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): OppgaveBehandlingstema =
        oppgaveBehandlingstemaFactory.utledOppgaveBehandlingstema(
            sakstype, sakstema, behandlingstema, behandlingstype
        )

    private val oppgaveBehandlingstemaFactory: OppgaveBehandlingstemaFactory
        get() = if (brukNyMapping())
            oppgaveBehandlingstemaFactoryNyMapping
        else
            oppgaveBehandlingstemaFactoryGammelMapping

    private fun brukNyMapping() = unleash.isEnabled(ToggleName.NY_GOSYS_MAPPING)
}
