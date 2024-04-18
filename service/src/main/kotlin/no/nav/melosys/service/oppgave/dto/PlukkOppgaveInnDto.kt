package no.nav.melosys.service.oppgave.dto

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema

@JvmRecord
data class PlukkOppgaveInnDto(
    @JvmField val sakstype: Sakstyper?,
    @JvmField val sakstema: Sakstemaer?,
    @JvmField val behandlingstema: Behandlingstema?
)
