package no.nav.melosys.service.oppgave.dto

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.Instant

data class BehandlingDto (
    @JvmField
    var behandlingID: Long? = null,
    var behandlingstype: Behandlingstyper? = null,
    var behandlingstema: Behandlingstema? = null,
    var behandlingsstatus: Behandlingsstatus? = null,
    var isErUnderOppdatering: Boolean = false,
    var registrertDato: Instant? = null,
    var endretDato: Instant? = null,
    var svarFrist: Instant? = null,
)
