package no.nav.melosys.config.observability

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.sdk.common.CompletableResultCode
import org.junit.jupiter.api.Test

class TraceparentTest {

    @Test
    fun `uten aktiv trace er det ingen traceparent eller traceId`() {
        Traceparent.nåværende().shouldBeNull()
        Traceparent.traceId().shouldBeNull()
    }

    @Test
    fun `aktiver gjenoppretter lagret traceparent som gjeldende kontekst`() {
        Traceparent.aktiver(TRACEPARENT).use {
            Traceparent.nåværende() shouldBe TRACEPARENT
            Traceparent.traceId() shouldBe TRACE_ID
        }
        Traceparent.nåværende().shouldBeNull()
    }

    @Test
    fun `aktiver uten traceparent endrer ikke konteksten`() {
        Traceparent.aktiver(null).use {
            Traceparent.nåværende().shouldBeNull()
        }
    }

    @Test
    fun `traceIdFra henter traceId fra traceparent`() {
        Traceparent.traceIdFra(TRACEPARENT) shouldBe TRACE_ID
        Traceparent.traceIdFra(null).shouldBeNull()
        Traceparent.traceIdFra("ugyldig").shouldBeNull()
    }

    @Test
    fun `iNyTrace gir ny traceId med lenke til jobbens trace`() {
        val (tracer, eksporterte) = testTracer()

        val traceparentIBlokk = Traceparent.aktiver(TRACEPARENT).use {
            Traceparent.iNyTrace(tracer, "test") { Traceparent.nåværende() }
        }

        val nyTraceId = Traceparent.traceIdFra(traceparentIBlokk).shouldNotBeNull()
        nyTraceId shouldNotBe TRACE_ID
        val span = eksporterte.single()
        span.traceId shouldBe nyTraceId
        span.parentSpanContext.isValid shouldBe false
        span.links shouldHaveSize 1
        span.links.single().spanContext.traceId shouldBe TRACE_ID
    }

    @Test
    fun `iNyTrace gjenoppretter jobbens trace etterpå`() {
        val (tracer, _) = testTracer()

        Traceparent.aktiver(TRACEPARENT).use {
            Traceparent.iNyTrace(tracer, "test") { }
            Traceparent.traceId() shouldBe TRACE_ID
        }
    }

    @Test
    fun `iNyTrace uten aktiv trace lager ingen lenke`() {
        val (tracer, eksporterte) = testTracer()

        Traceparent.iNyTrace(tracer, "test") { }

        eksporterte.single().links shouldHaveSize 0
    }

    @Test
    fun `iNyTrace markerer spanen som feilet og kaster videre`() {
        val (tracer, eksporterte) = testTracer()

        shouldThrow<IllegalStateException> {
            Traceparent.iNyTrace(tracer, "test") { error("feil") }
        }

        val span = eksporterte.single()
        span.status.statusCode shouldBe StatusCode.ERROR
        span.events.single().name shouldBe "exception"
    }

    @Test
    fun `iNyTrace setter attributter på spanen`() {
        val (tracer, eksporterte) = testTracer()

        Traceparent.iNyTrace(tracer, "test", mapOf("saksnummer" to "MEL-1")) { }

        eksporterte.single().attributes.get(AttributeKey.stringKey("saksnummer")) shouldBe "MEL-1"
    }

    @Test
    fun `iNyTrace uten OpenTelemetry kjører blokken uten trace og låser ikke global instans`() {
        GlobalOpenTelemetry.resetForTest()
        try {
            Traceparent.aktiver(TRACEPARENT).use {
                Traceparent.iNyTrace("test") { Traceparent.nåværende() }.shouldBeNull()
            }
            GlobalOpenTelemetry.isSet() shouldBe false
        } finally {
            GlobalOpenTelemetry.resetForTest()
        }
    }

    private fun testTracer(): Pair<io.opentelemetry.api.trace.Tracer, List<SpanData>> {
        val eksporterte = mutableListOf<SpanData>()
        val exporter = object : SpanExporter {
            override fun export(spans: Collection<SpanData>): CompletableResultCode {
                eksporterte.addAll(spans)
                return CompletableResultCode.ofSuccess()
            }

            override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()
            override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
        }
        val provider = SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build()
        return provider.get("test") to eksporterte
    }

    companion object {
        private const val TRACE_ID = "0af7651916cd43dd8448eb211c80319c"
        private const val TRACEPARENT = "00-$TRACE_ID-b7ad6b7169203331-01"
    }
}
