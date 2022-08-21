package no.nav.melosys.integrasjon.felles.mdc

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import io.kotest.matchers.string.shouldNotBeEqualIgnoringCase
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations.Companion.generateCallId
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations.Companion.getFromMDC
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations.Companion.putToMDC
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations.Companion.remove
import org.junit.jupiter.api.Test

class MDCOperationsTest {

    @Test
    fun test_generateCallId() {
        val callId1 = generateCallId()
        callId1.shouldNotBeNull()
        val callId2 = generateCallId()
        callId2.shouldNotBeNull()
            .shouldNotBeEqualIgnoringCase(callId1)
    }

    @Test
    fun test_mdc() {
        putToMDC("myKey", "myValue")
        getFromMDC("myKey")
            .shouldNotBeNull()
            .shouldBeEqualIgnoringCase("myValue")
        remove("myKey")
        getFromMDC("myKey").shouldBeNull()
    }

}
