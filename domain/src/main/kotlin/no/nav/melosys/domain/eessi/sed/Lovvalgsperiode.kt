package no.nav.melosys.domain.eessi.sed

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import java.time.LocalDate

class Lovvalgsperiode {

    var lovvalgsland: String? = null
    var unntakFraLovvalgsland: String? = null
    var bestemmelse: Bestemmelse? = null
    var tilleggsBestemmelse: Bestemmelse? = null

    @JsonDeserialize(using = LocalDateDeserializer::class)
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    var fom: LocalDate? = null

    @JsonDeserialize(using = LocalDateDeserializer::class)
    @JsonSerialize(using = LocalDateSerializer::class)
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd")
    var tom: LocalDate? = null

    var unntaksBegrunnelse: String? = null
    var unntakFraBestemmelse: Bestemmelse? = null

    fun tilPeriode(): Periode? {
        return if (fom != null && tom != null) {
            Periode(fom!!, tom!!)
        } else {
            null
        }
    }
}
