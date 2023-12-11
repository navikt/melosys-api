package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.OpphoertMedlemskapBrevbestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.time.LocalDate

class OpphoertMedlemskap(
    brevbestilling: OpphoertMedlemskapBrevbestilling,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val fristDato: LocalDate?,
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val opphoertDato: LocalDate?,
    val fritekst: String?,
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    class Builder(val brevbestilling: OpphoertMedlemskapBrevbestilling) {
        private val datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt)
        private var fritekst: String? = brevbestilling.fritekst
        private var fristDato: LocalDate? = null
        private var opphoertDato: LocalDate? = null

        fun fristDato(fristDato: LocalDate): Builder {
            this.fristDato = fristDato
            return this
        }

        fun opphoertDato(opphoertDato: LocalDate): Builder {
            this.opphoertDato = opphoertDato
            return this
        }

        fun build(): OpphoertMedlemskap {
            return OpphoertMedlemskap(
                brevbestilling,
                datoMottatt,
                fristDato,
                opphoertDato,
                fritekst
            )
        }
    }
}
