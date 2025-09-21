package no.nav.melosys.domain.dokument.inntekt

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.ErPeriode
import java.time.LocalDate

class Periode {

    private var _fom: LocalDate? = null
    private var _tom: LocalDate? = null

    constructor(fom: LocalDate?, tom: LocalDate?) {
        this._fom = fom
        this._tom = tom
    }

    var fom: LocalDate?
        get() = _fom
        set(value) { _fom = value }

    var tom: LocalDate?
        get() = _tom
        set(value) { _tom = value }

    // For JSON serialization - preserve original null values
    @JsonProperty("fom")
    fun getFomNullable(): LocalDate? = _fom

    @JsonProperty("tom")
    fun getTomNullable(): LocalDate? = _tom

    // Helper methods for cases where non-null is required
    fun hentFom(): LocalDate = _fom ?: error("fom er påkrevd for ${this::class.simpleName}")
    fun hentTom(): LocalDate = _tom ?: error("tom er påkrevd for ${this::class.simpleName}")

    // Convert to ErPeriode when fom is available
    fun tilErPeriode(): ErPeriode? = if (_fom != null) {
        object : ErPeriode {
            override var fom: LocalDate = this@Periode._fom!!
            override var tom: LocalDate? = this@Periode._tom
        }
    } else null

    override fun toString() = "${_fom ?: "null"} → ${_tom ?: "null"}"
}
