package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.Behandlingsnotat
import java.time.Instant

class BehandlingsnotatGetDto(
    behandlingsnotat: Behandlingsnotat, //
    val isRedigerbar: Boolean, val registrertAvNavn: String
) {
    val notatId: Long = behandlingsnotat.id //
    val tekst: String = behandlingsnotat.tekst //
    val endretDato: Instant = behandlingsnotat.endretDato
    val registrertDato: Instant = behandlingsnotat.registrertDato
    val behandlingstypeKode: String = behandlingsnotat.behandling.type.kode
    val behandlingstemaKode: String = behandlingsnotat.behandling.tema.kode
}
