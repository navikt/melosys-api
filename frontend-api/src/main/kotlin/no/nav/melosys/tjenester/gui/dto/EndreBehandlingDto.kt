package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import java.time.LocalDate

@JvmRecord
data class EndreBehandlingDto(
    val behandlingstype: Behandlingstyper,
    val behandlingstema: Behandlingstema,
    val behandlingsstatus: Behandlingsstatus,
    val mottaksdato: LocalDate
)
