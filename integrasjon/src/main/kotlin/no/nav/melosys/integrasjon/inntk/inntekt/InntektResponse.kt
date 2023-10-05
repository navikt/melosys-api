package no.nav.melosys.integrasjon.inntk.inntekt

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.melosys.exception.TekniskException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth

data class InntektResponse(
    val arbeidsInntektMaaned: List<ArbeidsInntektMaaned> = listOf(),
    val ident: Aktoer
) {
    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    fun tilJsonString(): String = try {
        objectMapper.writeValueAsString(this)
    } catch (e: JsonProcessingException) {
        throw TekniskException("Kunne ikke konvertere inntekt til json string", e)
    }

    data class ArbeidsInntektMaaned(
        val aarMaaned: YearMonth? = null,
        val avvikListe: List<Avvik>? = null,
        val arbeidsInntektInformasjon: ArbeidsInntektInformasjon? = null
    )

    data class ArbeidsInntektInformasjon(
        val arbeidsforholdListe: List<ArbeidsforholdFrilanser>? = null,
        val inntektListe: List<Inntekt>? = null,
        val forskuddstrekkListe: List<Forskuddstrekk>? = null,
        val fradragListe: List<Fradrag>? = null
    )

    data class Inntekt(
        val inntektType: InntektType? = null,
        val arbeidsforholdREF: String? = null,
        val beloep: BigDecimal,
        val fordel: String,
        val inntektskilde: String,
        val inntektsperiodetype: String,
        val inntektsstatus: String,
        val leveringstidspunkt: YearMonth,
        val opptjeningsland: String? = null,
        val opptjeningsperiodeFom: LocalDate? = null,
        val opptjeningsperiodeTom: LocalDate? = null,
        val skattemessigBosattLand: String? = null,
        val utbetaltIMaaned: YearMonth? = null,
        val opplysningspliktig: Aktoer? = null,
        val virksomhet: Aktoer? = null,
        val tilleggsinformasjon: Tilleggsinformasjon? = null,
        val inntektsmottaker: Aktoer? = null,
        val inngaarIGrunnlagForTrekk: Boolean? = null,
        val utloeserArbeidsgiveravgift: Boolean? = null,
        val informasjonsstatus: String? = null,
        val beskrivelse: String? = null,
        val skatteOgAvgiftsregel: String? = null,
        val antall: Int? = null
    )

    data class Tilleggsinformasjon(
        val kategori: String,
        val tilleggsinformasjonDetaljer: TilleggsinformasjonDetaljer
    )
    enum class InntektType {
        LOENNSINNTEKT,
        NAERINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE
    }

    data class ArbeidsforholdFrilanser(
        val antallTimerPerUkeSomEnFullStillingTilsvarer: Double? = null,
        val arbeidstidsordning: String? = null,
        val avloenningstype: String? = null,
        val sisteDatoForStillingsprosentendring: LocalDate? = null,
        val sisteLoennsendring: LocalDate? = null,
        val frilansPeriodeFom: LocalDate? = null,
        val frilansPeriodeTom: LocalDate? = null,
        val stillingsprosent: Double? = null,
        val yrke: String? = null,
        val arbeidsforholdID: String? = null,
        val arbeidsforholdIDnav: String? = null,
        val arbeidsforholdstype: String? = null,
        val arbeidsgiver: Aktoer? = null,
        val arbeidstaker: Aktoer? = null
    )

    data class Avvik(
        val ident: Aktoer? = null,
        val opplysningspliktig: Aktoer? = null,
        val virksomhet: Aktoer? = null,
        val avvikPeriode: YearMonth? = null,
        val tekst: String? = null
    )

    data class Forskuddstrekk(
        val beloep: Int = 0,
        val beskrivelse: String? = null,
        val leveringstidspunkt: LocalDateTime? = null,
        val opplysningspliktig: Aktoer? = null,
        val utbetaler: Aktoer? = null,
        val forskuddstrekkGjelder: Aktoer? = null
    )

    data class Fradrag(
        val beloep: BigDecimal? = null,
        val beskrivelse: String? = null,
        val fradragsperiode: YearMonth? = null,
        val leveringstidspunkt: LocalDateTime? = null,
        val inntektspliktig: Aktoer? = null,
        val utbetaler: Aktoer? = null,
        val fradragGjelder: Aktoer? = null
    )

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "detaljerType")
    @JsonSubTypes(
        JsonSubTypes.Type(value = AldersUfoereEtterlatteAvtalefestetOgKrigspensjon::class, name = AldersUfoereEtterlatteAvtalefestetOgKrigspensjon.TYPE),
        JsonSubTypes.Type(value = BarnepensjonOgUnderholdsbidrag::class, name = BarnepensjonOgUnderholdsbidrag.TYPE),
        JsonSubTypes.Type(value = BonusFraForsvaret::class, name = BonusFraForsvaret.TYPE),
        JsonSubTypes.Type(value = Etterbetalingsperiode::class, name = Etterbetalingsperiode.TYPE),
        JsonSubTypes.Type(value = Inntjeningsforhold::class, name = Inntjeningsforhold.TYPE),
        JsonSubTypes.Type(value = Svalbardinntekt::class, name = Svalbardinntekt.TYPE),
        JsonSubTypes.Type(value = ReiseKostOgLosji::class, name = ReiseKostOgLosji.TYPE)
    )
    open class TilleggsinformasjonDetaljer @JsonCreator constructor(
        @JsonIgnore val detaljerType: TilleggsinformasjonDetaljerType
    ) {
        companion object {
            const val TYPE = "TilleggsinformasjonDetaljer"
        }
    }

    data class AldersUfoereEtterlatteAvtalefestetOgKrigspensjon(
        val grunnpensjonbeloep: BigDecimal? = null,
        val heravEtterlattepensjon: BigDecimal? = null,
        val pensjonsgrad: Int? = null,
        val tidsromFom: LocalDate? = null,
        val tidsromTom: LocalDate? = null,
        val tilleggspensjonbeloep: BigDecimal? = null,
        val ufoeregradpensjonsgrad: Int? = null
    ) : TilleggsinformasjonDetaljer(TilleggsinformasjonDetaljerType.ALDERSUFOEREETTERLATTEAVTALEFESTETOGKRIGSPENSJON) {

        companion object {
            const val TYPE = "ALDERSUFOEREETTERLATTEAVTALEFESTETOGKRIGSPENSJON"
        }
    }

    data class BarnepensjonOgUnderholdsbidrag(
        val forsoergersFoedselnummer: String? = null,
        val tidsromFom: LocalDate? = null,
        val tidsromTom: LocalDate? = null
    ) : TilleggsinformasjonDetaljer(TilleggsinformasjonDetaljerType.BARNEPENSJONOGUNDERHOLDSBIDRAG) {

        companion object {
            const val TYPE = "BARNEPENSJONOGUNDERHOLDSBIDRAG"
        }
    }

    data class BonusFraForsvaret(
        val aaretUtbetalingenGjelderFor: Year? = null
    ) : TilleggsinformasjonDetaljer(TilleggsinformasjonDetaljerType.BONUSFRAFORSVARET) {

        companion object {
            const val TYPE = "BONUSFRAFORSVARET"
        }
    }

    data class Etterbetalingsperiode(
        val etterbetalingsperiodeFom: LocalDate? = null,
        val etterbetalingsperiodeTom: LocalDate? = null
    ) : TilleggsinformasjonDetaljer(TilleggsinformasjonDetaljerType.ETTERBETALINGSPERIODE) {

        companion object {
            const val TYPE = "ETTERBETALINGSPERIODE"
        }
    }

    data class Inntjeningsforhold(
        val spesielleInntjeningsforhold: String? = null
    ) : TilleggsinformasjonDetaljer(TilleggsinformasjonDetaljerType.INNTJENINGSFORHOLD) {

        companion object {
            const val TYPE = "INNTJENINGSFORHOLD"
        }
    }

    data class ReiseKostOgLosji(
        val persontype: String? = null
    ) : TilleggsinformasjonDetaljer(TilleggsinformasjonDetaljerType.REISEKOSTOGLOSJI) {

        companion object {
            const val TYPE = "REISEKOSTOGLOSJI"
        }
    }

    data class Svalbardinntekt(
        var antallDager : Int? = null,
        val betaltTrygdeavgift: BigDecimal? = null

    ) : TilleggsinformasjonDetaljer(TilleggsinformasjonDetaljerType.SVALBARDINNTEKT) {

        companion object {
            const val TYPE = "SVALBARDINNTEKT"
        }
    }

    enum class TilleggsinformasjonDetaljerType {
        ALDERSUFOEREETTERLATTEAVTALEFESTETOGKRIGSPENSJON,
        BARNEPENSJONOGUNDERHOLDSBIDRAG,
        BONUSFRAFORSVARET,
        ETTERBETALINGSPERIODE,
        INNTJENINGSFORHOLD,
        REISEKOSTOGLOSJI,
        SVALBARDINNTEKT
    }

}
