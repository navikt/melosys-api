package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.Medlemskapsperiode
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

    constructor(
        fom: LocalDate,
        tom: LocalDate,
        trygdedekning: Trygdedekninger,
        innvilgelsesResultat: InnvilgelsesResultat
    ) : this(fom, tom, trygdedekning, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, innvilgelsesResultat)

    companion object {
        fun av(medlemskapsperiode: Medlemskapsperiode): List<Periode> =
            medlemskapsperiode.trygdeavgiftsperioder.map {
                Periode(
                    it.periodeFra,
                    it.periodeTil,
                    medlemskapsperiode.trygdedekning,
                    it.trygdesats,
                    it.trygdeavgiftsbeløpMd.verdi,
                    it.grunnlagInntekstperiode.type,
                    it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi,
                    medlemskapsperiode.innvilgelsesresultat
                )
            }.toMutableList().also {
                if (!medlemskapsperiode.erInnvilget()) {
                    it.add(
                        Periode(
                            medlemskapsperiode.fom,
                            medlemskapsperiode.tom,
                            medlemskapsperiode.trygdedekning,
                            medlemskapsperiode.innvilgelsesresultat
                        )
                    )
                }
            }
    }
}
