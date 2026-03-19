package no.nav.melosys.integrasjon.felles

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.http.codec.CodecCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder

/**
 * Konfigurerer WebClient-codecs til å bruke Jackson 2 i stedet for standard Jackson 3.
 * Dette sikrer at Jackson 2-annotasjoner (f.eks. @JsonTypeIdResolver) virker korrekt
 * ved WebClient-deserialisering i Spring Boot 4.
 *
 * Bruker en dedikert ObjectMapper med Jackson-standardinnstillinger (ikke auto-konfigurert),
 * slik at LocalDate serialiseres som ISO-strenger — konsistent med produksjonskonfigurasjon.
 *
 * @Order(LOWEST_PRECEDENCE) sikrer at denne customizeren kjøres etter Spring Boot 4 sin
 * auto-konfigurerte JacksonJsonCodecConfiguration (Jackson 3), slik at Jackson 2 vinner.
 */
@Configuration
class WebClientJackson2CodecConfig {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun jackson2WebClientCodecCustomizer(): CodecCustomizer {
        val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule.Builder().build())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return CodecCustomizer { configurer ->
            configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
            configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
        }
    }
}
