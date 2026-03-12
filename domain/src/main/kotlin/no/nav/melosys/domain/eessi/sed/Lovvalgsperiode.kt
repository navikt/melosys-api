package no.nav.melosys.domain.eessi.sed

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape
import tools.jackson.databind.annotation.JsonDeserialize
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer
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

    fun tilPeriode(): Periode {
        return Periode(fom, tom)
    }
}
