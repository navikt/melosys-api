package no.nav.melosys.config.observability

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapSetter

/**
 * Lagrer og gjenoppretter W3C trace-kontekst (`traceparent`) for prosessering som skjer
 * utenfor den opprinnelige requesten, f.eks. prosessinstanser som kjøres eller restartes senere.
 * Da får loggene samme `trace_id` som opprinnelsen, og prosesseringen havner i samme trace i Tempo.
 */
object Traceparent {
    private const val TRACEPARENT = "traceparent"
    private const val INSTRUMENTASJON = "no.nav.melosys"
    private val propagator = W3CTraceContextPropagator.getInstance()

    @JvmStatic
    fun nåværende(): String? {
        val carrier = mutableMapOf<String, String>()
        propagator.inject(Context.current(), carrier, MapSetter)
        return carrier[TRACEPARENT]
    }

    @JvmStatic
    fun traceId(): String? =
        Span.current().spanContext.takeIf { it.isValid }?.traceId

    @JvmStatic
    fun traceIdFra(traceparent: String?): String? =
        traceparent?.split("-")?.getOrNull(1)?.takeIf { it.length == 32 }

    /** Gjør [traceparent] til gjeldende kontekst til returnert [Scope] lukkes. */
    @JvmStatic
    fun aktiver(traceparent: String?): Scope {
        if (traceparent == null) return Scope.noop()
        return propagator.extract(Context.current(), mapOf(TRACEPARENT to traceparent), MapGetter).makeCurrent()
    }

    /**
     * Kjører [blokk] i en ny trace, med en lenke tilbake til gjeldende trace.
     *
     * Brukes rundt opprettelsen av hver prosessinstans i batchjobber. OTel-agenten tar med
     * trace-konteksten inn i `@Async`-tråder, så uten dette deler alle prosessinstansene i en jobb
     * traceId med admin-kallet som startet den. Lenken gjør at man fortsatt finner jobben i Tempo.
     */
    @JvmStatic
    fun <T> iNyTrace(navn: String, blokk: () -> T): T =
        iNyTrace(GlobalOpenTelemetry.getTracer(INSTRUMENTASJON), navn, blokk)

    internal fun <T> iNyTrace(tracer: Tracer, navn: String, blokk: () -> T): T {
        val opphav = Span.current().spanContext
        val span = tracer.spanBuilder(navn)
            .setNoParent()
            .apply { if (opphav.isValid) addLink(opphav) }
            .startSpan()
        return try {
            span.makeCurrent().use { blokk() }
        } catch (e: Throwable) {
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            throw e
        } finally {
            span.end()
        }
    }

    private object MapSetter : TextMapSetter<MutableMap<String, String>> {
        override fun set(carrier: MutableMap<String, String>?, key: String, value: String) {
            carrier?.put(key, value)
        }
    }

    private object MapGetter : TextMapGetter<Map<String, String>> {
        override fun keys(carrier: Map<String, String>): Iterable<String> = carrier.keys
        override fun get(carrier: Map<String, String>?, key: String): String? = carrier?.get(key)
    }
}
