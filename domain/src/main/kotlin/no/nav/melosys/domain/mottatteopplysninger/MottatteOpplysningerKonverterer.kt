package no.nav.melosys.domain.mottatteopplysninger

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import java.util.*


object MottatteOpplysningerKonverterer {
    private val objectMapper = ObjectMapper()
    private val mapper = EnumMap<Mottatteopplysningertyper, Class<out MottatteOpplysningerData>>(
        Mottatteopplysningertyper::class.java
    )

    init {
        mapper[Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS] = SøknadNorgeEllerUtenforEØS::class.java
        mapper[Mottatteopplysningertyper.SØKNAD_IKKE_YRKESAKTIV] = SøknadIkkeYrkesaktiv::class.java
        mapper[Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS] = Soeknad::class.java
        mapper[Mottatteopplysningertyper.SØKNAD_A1_UTSENDTE_ARBEIDSTAKERE_EØS] = Soeknad::class.java
        mapper[Mottatteopplysningertyper.SED] = SedGrunnlag::class.java
        mapper[Mottatteopplysningertyper.ANMODNING_ELLER_ATTEST] = AnmodningEllerAttest::class.java
        objectMapper.registerModule(JavaTimeModule())
    }

    fun oppdaterMottatteOpplysninger(mottatteOpplysninger: MottatteOpplysninger) {
        if (mottatteOpplysninger.mottatteOpplysningerData != null) {
            try {
                mottatteOpplysninger.jsonData = lagJsonFraType(mottatteOpplysninger.mottatteOpplysningerData)
            } catch (e: JsonProcessingException) {
                throw IllegalArgumentException("Kan ikke lage json fra datagrunnlag. MottatteOpplysninger id: " + mottatteOpplysninger.id)
            }
        }
    }

    fun lastMottatteOpplysninger(mottatteOpplysninger: MottatteOpplysninger) {
        try {
            mottatteOpplysninger.mottatteOpplysningerData =
                lagDatagrunnlagFraType(mottatteOpplysninger.jsonData, klasseForType(mottatteOpplysninger.type))
        } catch (e: JsonProcessingException) {
            throw IllegalStateException("Kan ikke laste datagrunnlag med id " + mottatteOpplysninger.id, e)
        }
    }

    fun klasseForType(type: Mottatteopplysningertyper): Class<out MottatteOpplysningerData> {
        return mapper[type]!!
    }

    @Throws(JsonProcessingException::class)
    private fun lagDatagrunnlagFraType(json: String, clazz: Class<out MottatteOpplysningerData>): MottatteOpplysningerData {
        return objectMapper.readValue(json, clazz)
    }

    @Throws(JsonProcessingException::class)
    private fun lagJsonFraType(mottatteOpplysningerData: MottatteOpplysningerData): String {
        return objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(mottatteOpplysningerData)
    }
}

