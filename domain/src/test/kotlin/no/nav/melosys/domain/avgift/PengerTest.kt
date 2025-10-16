package no.nav.melosys.domain.avgift

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PengerTest {

    @Test
    fun `equals should return true for two Penger with same null values`() {
        val penger1 = Penger(null, "NOK")
        val penger2 = Penger(null, "NOK")

        penger1 shouldBe penger2
    }
}
