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

/**
 * Avviser tall som `LocalDate` i request-body. Jackson tolker ellers `12345` som epoch-day.
 * Regelen settes på MVC-converteren, ikke på den delte `JsonMapper`-beanen, så Kafka-consumerne
 * er upåvirket.
 */
@Configuration
class JsonDatoKoersjonConfig(private val jsonMapper: ObjectFactory<JsonMapper>) : WebMvcConfigurer {

    override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
        builder.withJsonConverter(JacksonJsonHttpMessageConverter(mvcJsonMapper()))
        // Ellers kan samme request sendes som YAML eller XML og treffe en mapper uten regelen.
        builder.configureMessageConvertersList { it.removeIf(::erAlternativtJacksonFormat) }
    }

    private fun erAlternativtJacksonFormat(converter: HttpMessageConverter<*>) =
        converter is AbstractJacksonHttpMessageConverter<*> && converter !is JacksonJsonHttpMessageConverter

    private fun mvcJsonMapper(): JsonMapper =
        jsonMapper.getObject().rebuild()
            .withCoercionConfig(LocalDate::class.java) { it.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail) }
            .build()
}
