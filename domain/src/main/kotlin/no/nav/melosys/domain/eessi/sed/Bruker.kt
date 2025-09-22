package no.nav.melosys.domain.eessi.sed

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import java.time.LocalDate

class Bruker {
    var fornavn: String? = null
    var etternavn: String? = null

    @JsonDeserialize(using = LocalDateDeserializer::class)
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    var foedseldato: LocalDate? = null

    var kjoenn: String? = null
    var statsborgerskap: Collection<String>? = null
    var fnr: String? = null
    var harSensitiveOpplysninger: Boolean = false
}
