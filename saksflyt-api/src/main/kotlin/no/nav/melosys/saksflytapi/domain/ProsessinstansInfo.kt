package no.nav.melosys.saksflytapi.domain

import java.time.LocalDateTime
import java.util.*

class ProsessinstansInfo(
    val uuid: UUID,
    val prosessStatus: ProsessStatus,
    val registrertDato: LocalDateTime,
    val låsReferanse: String
)
