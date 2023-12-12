package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.brev.VedtakOpphoertMedlemskapBrevbestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.time.LocalDate

class VedtakOpphoertMedlemskap(
    brevbestilling: VedtakOpphoertMedlemskapBrevbestilling,

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val datoMottatt: LocalDate?,

    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val opphoertDato: LocalDate?,

    val fritekst: String?,
) : DokgenDto(brevbestilling, Mottakerroller.BRUKER) {

    class Builder(val brevbestilling: VedtakOpphoertMedlemskapBrevbestilling) {
        private val datoMottatt = instantTilLocalDate(brevbestilling.forsendelseMottatt)
        private var fritekst: String? = brevbestilling.opphoertBegrunnelseFritekst
        private var opphoertDato: LocalDate? = null

        fun opphoertDato(opphoertDato: LocalDate): Builder {
            this.opphoertDato = opphoertDato
            return this
        }

        fun build(): VedtakOpphoertMedlemskap {
            return VedtakOpphoertMedlemskap(
                brevbestilling,
                datoMottatt,
                opphoertDato,
                fritekst
            )
        }
    }
}
