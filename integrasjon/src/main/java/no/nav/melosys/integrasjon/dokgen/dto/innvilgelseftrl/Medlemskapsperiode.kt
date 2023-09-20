package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class Medlemskapsperiode(
    @JsonSerialize(using = LocalDateSerializer::class) val fom: LocalDate,
    @JsonSerialize(
        using = LocalDateSerializer::class
    ) val tom: LocalDate,
    val trygdedekning: Trygdedekninger,
    val innvilgelsesResultat: InnvilgelsesResultat
)
