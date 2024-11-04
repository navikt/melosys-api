package no.nav.melosys.integrasjon

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.string.shouldContain
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention
import org.springframework.web.reactive.function.client.WebClient


@TestConfiguration
class MetricsTestConfig() {

    @Bean
    fun myBuilder(): WebClient.Builder {
        return WebClient.builder()
            .observationRegistry(observationRegistry())
            .observationConvention(CustomWebClientObservationConvention())
    }

    @Bean
    fun observationRegistry(): ObservationRegistry {
        val observationRegistry = ObservationRegistry.create()
        observationRegistry.observationConfig()
            .observationHandler(DefaultMeterObservationHandler(meterRegistry))
        return observationRegistry
    }


    class CustomWebClientObservationConvention : DefaultClientRequestObservationConvention() {
        override fun getName(): String {
            return "test"
        }

    }

    companion object {
        var meterRegistry = SimpleMeterRegistry()
        fun clearMeterRegistry() {
            meterRegistry.clear()
        }

        fun checkMetricsUri(uri: String) {
            meterRegistry.meters
                .map { it.id.getTag("uri") }
                .shouldContain(uri)
        }

        fun metricsUriShouldContainBrackets() {
            meterRegistry.meters.first { it.id.name == "test" }.id.apply {
                    this.getTag("uri")
                        .shouldContain("{")
                }
        }
    }
}
