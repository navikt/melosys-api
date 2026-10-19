package no.nav.melosys.config.observability

import io.opentelemetry.api.trace.Span
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
