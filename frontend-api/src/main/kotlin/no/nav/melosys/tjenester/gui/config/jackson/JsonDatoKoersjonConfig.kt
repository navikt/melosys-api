package no.nav.melosys.tjenester.gui.config.jackson

import org.springframework.beans.factory.ObjectFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.cfg.CoercionAction
import tools.jackson.databind.cfg.CoercionInputShape
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Avviser tall som `LocalDate`/`LocalDateTime` i HTTP request-body. Uten denne regelen tolker Jackson
 * `{"periodeFom": 12345}` som epoch-day (2003-10-20) i stedet for å avvise verdien.
 *
 * Regelen settes kun på MVC sin JSON-converter, ikke på den delte `JsonMapper`-beanen. Den delte
 * mapperen brukes også av Kafka-consumerne via `KafkaConfig.LoggingDeserializer`, og
 * `SkippableKafkaErrorHandler` arver `CommonContainerStoppingErrorHandler` – en deserialiseringsfeil
 * der stopper containeren til meldingen manuelt merkes for skipping. Problemet er rent
 * frontend-vendt, så rekkevidden holdes til HTTP.
 *
 * `rebuild()` kopierer konfigurasjonen fra den delte mapperen uten å endre den, slik at moduler
 * (`KotlinModule`, [MelosysModule]) og features er identiske bortsett fra denne ene regelen.
 *
 * Gjelder kun `LocalDate`/`LocalDateTime`. For `Instant`/`OffsetDateTime` er tall en gyldig
 * epoch-representasjon.
 */
@Configuration
class JsonDatoKoersjonConfig(private val jsonMapper: ObjectFactory<JsonMapper>) : WebMvcConfigurer {

    override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
        builder.withJsonConverter(JacksonJsonHttpMessageConverter(mvcJsonMapper()))
        // Uten dette kan samme request sendes som YAML/XML og treffe en mapper uten regelen over.
        // Frontend sender kun JSON. Dekket av ValideringUnntaksperiodeControllerTest.
        builder.configureMessageConvertersList { it.removeIf(::erAlternativtJacksonFormat) }
    }

    private fun erAlternativtJacksonFormat(converter: HttpMessageConverter<*>) =
        converter is AbstractJacksonHttpMessageConverter<*> && converter !is JacksonJsonHttpMessageConverter

    private fun mvcJsonMapper(): JsonMapper =
        jsonMapper.getObject().rebuild()
            .withCoercionConfig(LocalDate::class.java) { it.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail) }
            .withCoercionConfig(LocalDateTime::class.java) { it.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail) }
            .build()
}
