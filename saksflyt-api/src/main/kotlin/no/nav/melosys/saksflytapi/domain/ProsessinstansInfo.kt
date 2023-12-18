package no.nav.melosys.saksflytapi.domain

import java.time.LocalDateTime
import java.util.*

class ProsessinstansInfo(
    val uuid: UUID,
    val prosessStatus: ProsessStatus,
    val registrertDato: LocalDateTime,
    val låsReferanse: String
) {
    val sedLåsReferanse: SedLåsReferanse? = if (SedLåsReferanse.erGyldigReferanse(låsReferanse)) SedLåsReferanse(
        låsReferanse
    ) else null

    // Kun brukt fra test TODO: flytt denne koden til test
    constructor(prosessinstans: Prosessinstans) : this(
        prosessinstans.id!!, prosessinstans.status!!,
        prosessinstans.registrertDato!!, prosessinstans.låsReferanse!!
    )
}
