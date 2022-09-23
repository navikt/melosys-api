package no.nav.melosys.service.sak

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

data class EndreSakDto(
    val sakstype: Sakstyper,
    val sakstema: Sakstemaer,
    val behandlingstema: Behandlingstema,
    val behandlingstype: Behandlingstyper
)
