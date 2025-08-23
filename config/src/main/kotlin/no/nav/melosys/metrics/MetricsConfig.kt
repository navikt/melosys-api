package no.nav.melosys.metrics

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusRenameFilter
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig {
    @Bean
    fun metricsCommonTags() = MeterRegistryCustomizer<MeterRegistry> { registry: MeterRegistry ->
        registry.config().meterFilter(PrometheusRenameFilter())
            .commonTags("app", "melosys", "team", "teammelosys")
    }

    @Bean
    fun processMemoryMetrics(): MeterBinder = ProcessMemoryMetrics()

    @Bean
    fun processThreadMetrics(): MeterBinder = ProcessThreadMetrics()
}
