//package no.nav.melosys.integrasjon
//
//import io.kotest.matchers.collections.shouldContain
//import io.kotest.matchers.collections.shouldHaveSize
//import io.kotest.matchers.shouldBe
//import io.kotest.matchers.string.shouldContain
//import io.micrometer.common.KeyValue
//import io.micrometer.common.KeyValues
//import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler
//import io.micrometer.core.instrument.simple.SimpleMeterRegistry
//import io.micrometer.observation.GlobalObservationConvention
//import io.micrometer.observation.ObservationRegistry
//import org.springframework.boot.test.context.TestConfiguration
//import org.springframework.context.annotation.Bean
//import org.springframework.http.server.reactive.observation.ServerHttpObservationDocumentation
//import org.springframework.web.reactive.function.client.ClientHttpObservationDocumentation
//import org.springframework.web.reactive.function.client.ClientRequestObservationContext
//import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention
//import org.springframework.web.reactive.function.client.WebClient
//
//
//@TestConfiguration
//class MetricsTestConfig() {
//
//https://github.com/spring-projects/spring-boot/issues/34009
//    @Bean
//    fun myBuilder(): WebClient.Builder {
//        val metricsWebClientFilterFunction = MetricsWebClientFilterFunction(
//            meterRegistry, DefaultWebClientExchangeTagsProvider(), "test", AutoTimer.ENABLED
//        )
//        return WebClient.builder().filters { it.add((metricsWebClientFilterFunction)) }
//    }
//
//    @Bean
//    fun myBuilder(): WebClient.Builder {
//        return WebClient.builder()
//            .observationRegistry(observationRegistry())
//            .observationConvention(CustomWebClientObservationConvention())
//    }
//
//    @Bean
//    fun observationRegistry(): ObservationRegistry {
//        val observationRegistry = ObservationRegistry.create()
//        observationRegistry.observationConfig()
//            .observationHandler(DefaultMeterObservationHandler(meterRegistry))
//        return observationRegistry
//    }
//
//
//    class CustomWebClientObservationConvention : DefaultClientRequestObservationConvention(),
//        GlobalObservationConvention<ClientRequestObservationContext?> {
//        override fun getLowCardinalityKeyValues(context: ClientRequestObservationContext): KeyValues {
//            val lowCardinalityKeyValues = super<DefaultClientRequestObservationConvention>.getLowCardinalityKeyValues(context)
//            val statusKeyValue: KeyValue = lowCardinalityKeyValues.stream()
//                .filter { keyValue: KeyValue -> keyValue.key == ClientHttpObservationDocumentation.LowCardinalityKeyNames.STATUS.asString() }
//                .findAny()
//                .get()
//            val uriKeyValue: KeyValue = lowCardinalityKeyValues.stream()
//                .filter { keyValue: KeyValue -> keyValue.key == ClientHttpObservationDocumentation.LowCardinalityKeyNames.URI.asString() }
//                .findAny()
//                .get()
//            return KeyValues.of(statusKeyValue, uriKeyValue)
//        }
//
//        override fun getHighCardinalityKeyValues(context: ClientRequestObservationContext): KeyValues {
//            val highCardinalityKeyValues = super<DefaultClientRequestObservationConvention>.getHighCardinalityKeyValues(context)
//            val clientNameKeyValue: KeyValue = highCardinalityKeyValues.stream()
//                .filter { keyValue: KeyValue -> keyValue.key == ServerHttpObservationDocumentation.HighCardinalityKeyNames.HTTP_URL.asString() }
//                .findFirst()
//                .get()
//            return KeyValues.of(clientNameKeyValue)
//        }
//
//        override fun getName(): String {
//            return "test" // the name of my metric
//        }
//    }
//
//
//    companion object {
//        var meterRegistry = SimpleMeterRegistry()
//        fun clearMeterRegistry() {
//            meterRegistry.clear()
//        }
//
//        fun checkMetricsUri(uri: String) {
//            meterRegistry.meters
//                .map { it.id.getTag("uri") }
//                .shouldContain(uri)
//        }
//
//        fun metricsUriShouldContainBrackets() {
//            meterRegistry.meters
//                .shouldHaveSize(1)
//                .first().id.apply {
//                    name.shouldBe("test")
//                    this.getTag("uri")
//                        .shouldContain("{")
//                }
//        }
//    }
//}
