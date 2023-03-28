package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema

data class LovvalgsbestemmelserRequest(
    val sakstema: Sakstemaer,
    val behandlingstema: Behandlingstema,
    val land: Land_iso2
)
