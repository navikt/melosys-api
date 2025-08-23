package no.nav.melosys.saksflyt.metrikker

import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType

data class ProsessinstansStegAntall(
    val sistFullfortSteg: ProsessSteg?,
    val prosessType: ProsessType,
    val prosessStatus: ProsessStatus,
    val antall: Long
)
