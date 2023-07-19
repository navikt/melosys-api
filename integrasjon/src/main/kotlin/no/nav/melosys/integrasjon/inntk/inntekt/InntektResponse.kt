package no.nav.melosys.integrasjon.inntk.inntekt

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class InntektResponse(
    val arbeidsInntektMaaned: List<ArbeidsInntektMaaned>? = null,
    val ident: Aktoer
)
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
    val beloep: BigDecimal? = null,
    val fordel: String? = null,
    val inntektskilde: String? = null,
    val inntektsperiodetype: String? = null,
    val inntektsstatus: String? = null,
    val leveringstidspunkt: YearMonth? = null,
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
    val kategori: String? = null,
    val tilleggsinformasjonDetaljer: TilleggsinformasjonDetaljer? = null
)

class TilleggsinformasjonDetaljer(
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
