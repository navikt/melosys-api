package no.nav.melosys.saksflyt.metrikker

import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType


data class ProsessinstansAntall(
    val prosessType: ProsessType,
    val prosessStatus: ProsessStatus,
    val antall: Long
)
