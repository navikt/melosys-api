package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import java.time.Instant

data class BehandlingOversiktDto(
    val behandlingID: Long,
    val tittel: String,
    val behandlingsstatus: Behandlingsstatus,
    val behandlingstype: Behandlingstyper,
    val behandlingstema: Behandlingstema,
    val soknadsperiode: PeriodeDto?,
    val opprettetDato: Instant,
    val behandlingsresultattype: Behandlingsresultattyper,
    val svarFrist: Instant?
)
