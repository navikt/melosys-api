package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
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

    data class Arbeidsforhold(
        private val unknownProperties: MutableMap<String?, Any> = mutableMapOf(),
        val arbeidsforholdId: String, // Arbeidsforhold-id fra opplysningspliktig
        val navArbeidsforholdId: Int, // Arbeidsforhold-id i AAREG
        val ansettelsesperiode: Ansettelsesperiode,
        val type: String?,
        val arbeidstaker: Arbeidstaker,
        val arbeidsavtaler: List<Arbeidsavtale>?,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        val permisjonPermitteringer: List<PermisjonPermittering>,
        val utenlandsopphold: List<Utenlandsopphold>,
        val arbeidsgiver: Arbeidsgiver,
        val opplysningspliktig: Opplysningspliktig,
        val innrapportertEtterAOrdningen: Boolean?,
        val registrert: String?,
        val sistBekreftet: String?,
        val antallTimerForTimeloennet: List<AntallTimerForTimeloennet>
    ) {
        val periode: Periode
            get() = ansettelsesperiode.periode

        val opplysningspliktigtype: String
            get() = opplysningspliktig.type.uppercase(Locale.getDefault())

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
        val rapporteringsperiode: String
    )

    data class Opplysningspliktig(
        val type: String,  //  Organisasjon eller Person
        val organisasjonsnummer: String? // Ligger i respons fra service, men ikke i swagger doc.
    )

    data class Arbeidsgiver(
        val type: String,
        val organisasjonsnummer: String?
    )

    data class Utenlandsopphold(val landkode: String?, val periode: Periode, val rapporteringsperiode: String)

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
