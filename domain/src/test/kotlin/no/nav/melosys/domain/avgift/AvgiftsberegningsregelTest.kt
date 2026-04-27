package no.nav.melosys.domain.avgift

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AvgiftsberegningsregelTest {

    @Test
    fun `parse aksepterer kanoniske norske enum-navn`() {
        Avgiftsberegningsregel.parse("ORDINÆR") shouldBe Avgiftsberegningsregel.ORDINÆR
        Avgiftsberegningsregel.parse("TJUEFEM_PROSENT_REGEL") shouldBe Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL
        Avgiftsberegningsregel.parse("MINSTEBELØP") shouldBe Avgiftsberegningsregel.MINSTEBELØP
    }

    @Test
    fun `parse aksepterer ASCII-aliaser fra eldre upstream-versjoner`() {
        Avgiftsberegningsregel.parse("ORDINAER") shouldBe Avgiftsberegningsregel.ORDINÆR
        Avgiftsberegningsregel.parse("MINSTEBELOEP") shouldBe Avgiftsberegningsregel.MINSTEBELØP
    }

    @Test
    fun `parse normaliserer til store bokstaver`() {
        Avgiftsberegningsregel.parse("ordinaer") shouldBe Avgiftsberegningsregel.ORDINÆR
        Avgiftsberegningsregel.parse("minstebeloep") shouldBe Avgiftsberegningsregel.MINSTEBELØP
    }

    @Test
    fun `parse kaster IllegalArgumentException for ukjente verdier`() {
        shouldThrow<IllegalArgumentException> {
            Avgiftsberegningsregel.parse("UKJENT_REGEL")
        }
    }
}
