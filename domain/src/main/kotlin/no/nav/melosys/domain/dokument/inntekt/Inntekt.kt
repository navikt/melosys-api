package no.nav.melosys.domain.dokument.inntekt

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.dokument.DokumentView
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

@JsonPropertyOrder(
    "type",
    "arbeidsforholdREF",
    "beloep",
    "fordel",
    "inntektskilde",
    "inntektsperiodetype",
    "inntektsstatus",
    "levereringstidspunkt",
    "opptjeningsland",
    "opptjeningsperiode",
    "skattemessigBosattLand",
    "utbetaltIPeriode",
    "opplysningspliktigID",
    "inntektsinnsenderID",
    "virksomhetID",
    "tilleggsinformasjon",
    "inntektsmottakerID",
    "inngaarIGrunnlagForTrekk",
    "utloeserArbeidsgiveravgift",
    "informasjonsstatus",
    "beskrivelse",
    "antall"
)
data class Inntekt(
    var type: InntektType,
    @JsonView(DokumentView.Database::class)
    var arbeidsforholdREF: String? = null,
    val beloep: BigDecimal,
    val fordel: String, //Fordel http://nav.no/kodeverk/Kodeverk/Fordel
    val inntektskilde: String, //"http://nav.no/kodeverk/Kodeverk/InntektsInformasjonsopphav"
    val inntektsperiodetype: String, //http://nav.no/kodeverk/Kodeverk/Inntektsperiodetyper
    val inntektsstatus: String, //"http://nav.no/kodeverk/Kodeverk/Inntektsstatuser"
    var levereringstidspunkt: LocalDateTime? = null,
    var opptjeningsland: String? = null,
    var opptjeningsperiode: Periode? = null,

    @JsonView(DokumentView.Database::class)
    var skattemessigBosattLand: String? = null,

    val utbetaltIPeriode: YearMonth,
    var opplysningspliktigID: String? = null,

    @JsonView(DokumentView.Database::class)
    var inntektsinnsenderID: String? = null,
    var virksomhetID: String? = null,

    @JsonView(DokumentView.Database::class)
    var tilleggsinformasjon: Tilleggsinformasjon? = null,
    var inntektsmottakerID: String? = null,
    var inngaarIGrunnlagForTrekk: Boolean? = null,
    var utloeserArbeidsgiveravgift: Boolean? = null,
    var informasjonsstatus: String? = null, //"http://nav.no/kodeverk/Kodeverk/Informasjonsstatuser"
    var beskrivelse: String? = null,

    @JsonView(DokumentView.Database::class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    var antall: Int? = null
)
