package no.nav.melosys.service.avgift.dto

import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import java.time.LocalDate
import java.util.*

data class SkatteforholdTilNorgeRequest(
    val id: UUID,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val skatteplikttype: Skatteplikttype
) {
    constructor(fomDato: LocalDate, tomDato: LocalDate, skatteplikttype: Skatteplikttype) : this(
        id = UUID.nameUUIDFromBytes("${fomDato}${tomDato}${skatteplikttype}".toByteArray()),
        fomDato = fomDato,
        tomDato = tomDato,
        skatteplikttype = skatteplikttype
    )

    // TODO remove if not needed
    constructor(skatteforholdTilNorge: SkatteforholdTilNorge) : this(
        fomDato = skatteforholdTilNorge.fomDato,
        tomDato = skatteforholdTilNorge.tomDato,
        skatteplikttype = skatteforholdTilNorge.skatteplikttype
    )

    companion object {
        fun SkatteforholdTilNorgeRequest.toUUID(): UUID {                                                           // TODO undersøk nærmere hvor viktig det er beholde det her.. lå tidligere i trygdeavgiftsberegningservice
            return UUID.nameUUIDFromBytes("${this.fomDato}${this.tomDato}${this.skatteplikttype}".toByteArray())
        }

        fun tilSkatteforhold(skatteforholdTilNorgeRequest: SkatteforholdTilNorgeRequest) = SkatteforholdTilNorge().apply {
            this.fomDato = skatteforholdTilNorgeRequest.fomDato
            this.tomDato = skatteforholdTilNorgeRequest.tomDato
            this.skatteplikttype = skatteforholdTilNorgeRequest.skatteplikttype
        }
    }
}
