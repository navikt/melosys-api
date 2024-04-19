package no.nav.melosys.service.oppgave.dto

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.Instant

data class BehandlingDto (
    val behandlingID: Long,
    val behandlingstype: Behandlingstyper,
    val behandlingstema: Behandlingstema,
    val behandlingsstatus: Behandlingsstatus,
    val isErUnderOppdatering: Boolean = false,
    val registrertDato: Instant,
    val endretDato: Instant,
    val svarFrist: Instant? = null,
)
