package no.nav.melosys.config.observability

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
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

    companion object {
        private const val TRACE_ID = "0af7651916cd43dd8448eb211c80319c"
        private const val TRACEPARENT = "00-$TRACE_ID-b7ad6b7169203331-01"
    }
}
