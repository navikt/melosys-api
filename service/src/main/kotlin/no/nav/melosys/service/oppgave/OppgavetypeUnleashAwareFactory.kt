package no.nav.melosys.service.oppgave

import no.finn.unleash.Unleash
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.featuretoggle.ToggleName

class OppgavetypeUnleashAwareFactory(private val unleash: Unleash) : OppgavetypeFactory {
    private val oppgavetypeGammelMappingFactory: OppgavetypeFactory by lazy {
        OppgavetypeGammelMappingFactory()
    }
    private val oppgavetypeNyMappingFactory: OppgavetypeFactory by lazy {
        OppgavetypeNyMappingFactory()
    }

    override fun utledOppgavetype(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Oppgavetyper = oppgavetypeFactory.utledOppgavetype(sakstype, behandlingstema, behandlingstype)

    private val oppgavetypeFactory: OppgavetypeFactory
        get() = if (brukNyMapping())
            oppgavetypeNyMappingFactory
        else
            oppgavetypeGammelMappingFactory

    private fun brukNyMapping() = unleash.isEnabled(ToggleName.NY_GOSYS_MAPPING)
}
