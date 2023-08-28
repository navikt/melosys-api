package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.math.BigDecimal
import java.time.LocalDate

class Periode(
    @JsonSerialize(using = LocalDateSerializer::class) val fom: LocalDate,
    @JsonSerialize(
        using = LocalDateSerializer::class
    ) val tom: LocalDate,
    val trygdedekning: Trygdedekninger,
    val avgiftssats: BigDecimal,
    val avgiftPerMd: BigDecimal,
    val inntektskildetype: Inntektskildetype?,
    val avgiftspliktigInntektPerMd: BigDecimal,
    val innvilgelsesResultat: InnvilgelsesResultat
) {
    companion object {
        fun av(trygdeavgiftsperiode: Trygdeavgiftsperiode): Periode =
            Periode(
                trygdeavgiftsperiode.periodeFra,
                trygdeavgiftsperiode.periodeTil,
                trygdeavgiftsperiode.grunnlagMedlemskapsperiode.trygdedekning,
                trygdeavgiftsperiode.trygdesats,
                trygdeavgiftsperiode.trygdeavgiftsbeløpMd?.verdi ?: BigDecimal.ZERO,
                trygdeavgiftsperiode.grunnlagInntekstperiode.type,
                trygdeavgiftsperiode.grunnlagInntekstperiode.avgiftspliktigInntektMnd?.verdi ?: BigDecimal.ZERO,
                trygdeavgiftsperiode.grunnlagMedlemskapsperiode.innvilgelsesresultat
            )

        fun av(medlemskapsperiode: Medlemskapsperiode): Periode =
            Periode(
                medlemskapsperiode.fom,
                medlemskapsperiode.tom,
                medlemskapsperiode.trygdedekning,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                medlemskapsperiode.innvilgelsesresultat
            )
    }
}
