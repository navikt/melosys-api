package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl

import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class MedlemskapsperiodeDto(
    @JsonSerialize(using = LocalDateSerializer::class) val fom: LocalDate,
    @JsonSerialize(using = LocalDateSerializer::class) val tom: LocalDate?,
    val trygdedekning: Trygdedekninger,
    val innvilgelsesResultat: InnvilgelsesResultat
)
