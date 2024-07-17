package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.melosys.exception.TekniskException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class ArbeidsforholdResponse(val arbeidsforhold: List<Arbeidsforhold>) {
    fun tilSaksopplysning(): String {
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
        try {
            // serialize to json or find out if we want to convert to xml doc?
            return objectMapper.writeValueAsString(arbeidsforhold)
        } catch (e: JsonProcessingException) {
            throw TekniskException("Kunne ikke konvertere arbeidsforhold til json string", e)
        }
    }

    class Arbeidsforhold {
        private val unknownProperties: MutableMap<String?, Any> = HashMap()

        @JsonProperty
        var arbeidsforholdId: String? = null // Arbeidsforhold-id fra opplysningspliktig

        @JsonProperty
        var navArbeidsforholdId: Int? = null // Arbeidsforhold-id i AAREG

        @JsonProperty
        var ansettelsesperiode: Ansettelsesperiode? = null

        @JsonProperty
        var type: String? = null

        @JsonProperty
        var arbeidstaker: Arbeidstaker? = null

        @JsonProperty
        var arbeidsavtaler: List<Arbeidsavtale>? = null

        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty
        var permisjonPermitteringer: List<PermisjonPermittering>? = null

        @JsonProperty
        var utenlandsopphold: List<Utenlandsopphold>? = null

        @JsonProperty
        var arbeidsgiver: Arbeidsgiver? = null

        @JsonProperty
        var opplysningspliktig: Opplysningspliktig? = null

        @JsonProperty
        var innrapportertEtterAOrdningen: Boolean? = null

        @JsonProperty
        var registrert: String? = null

        @JsonProperty
        var sistBekreftet: String? = null

        @JsonProperty
        var antallTimerForTimeloennet: List<AntallTimerForTimeloennet>? = null

        val periode: Periode
            get() = ansettelsesperiode!!.periode

        val opplysningspliktigtype: String
            get() = opplysningspliktig!!.type!!.uppercase(Locale.getDefault())

        @JsonAnySetter
        fun setUnknownProperty(key: String?, value: Any) {
            unknownProperties[key] = value
        }

        @JsonAnyGetter
        fun getUnknownProperty(key: String): Any? {
            return unknownProperties[key]
        }
    }

    data class AntallTimerForTimeloennet(
        val antallTimer: BigDecimal?,
        val periode: Periode,
        val rapporteringsperiode: String?
    )

    data class Opplysningspliktig(
        val type: String,  //  Organisasjon eller Person
        val organisasjonsnummer: String? // Ligger i respons fra service, men ikke i swagger doc.
    )

    data class Arbeidsgiver(
        val type: String,
        val organisasjonsnummer: String?
    )

    data class Utenlandsopphold(val landkode: String?, val periode: Periode, val rapporteringsperiode: String?)

    data class Ansettelsesperiode(val periode: Periode)

    data class Arbeidstaker(val type: String?, val offentligIdent: String?, val aktoerId: String?)

    data class Periode(val fom: LocalDate, val tom: LocalDate?)

    data class PermisjonPermittering(
        val periode: Periode,
        val permisjonPermitteringId: String?,
        val prosent: BigDecimal?,
        val type: String?,  // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/PermisjonsOgPermitteringsBeskrivelse
        val varslingskode: String
    )

    data class Arbeidsavtale(
        val type: String?,  // Type for arbeidsavtale - Forenklet, Frilanser, Maritim, Ordinaer
        val arbeidstidsordning: String?,  // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Arbeidstidsordninger
        val yrke: String?,  // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/Yrker
        val ansettelsesform: String?,  // https://kodeverk-web.dev.adeo.no/kodeverksoversikt/kodeverk/AnsettelsesformAareg
        val stillingsprosent: BigDecimal?,
        val beregnetAntallTimerPrUke: BigDecimal?,
        val gyldighetsperiode: Periode,
        val sistStillingsendring: LocalDate?,
        val sistLoennsendring: LocalDate?,
        val antallTimerPrUke: BigDecimal?
    )
}
