package no.nav.melosys.integrasjon

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.actuate.metrics.AutoTimer
import org.springframework.boot.actuate.metrics.web.reactive.client.DefaultWebClientExchangeTagsProvider
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientFilterFunction
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient

@TestConfiguration
class MetricsTestConfig() {

    @Bean
    fun myBuilder(): WebClient.Builder {
        val metricsWebClientFilterFunction = MetricsWebClientFilterFunction(
            meterRegistry, DefaultWebClientExchangeTagsProvider(), "test", AutoTimer.ENABLED
        )
        return WebClient.builder().filters { it.add((metricsWebClientFilterFunction)) }
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
            meterRegistry.meters
                .shouldHaveSize(1)
                .first().id.apply {
                    name.shouldBe("test")
                    this.getTag("uri")
                        .shouldContain("{")
                }
        }
    }
}
