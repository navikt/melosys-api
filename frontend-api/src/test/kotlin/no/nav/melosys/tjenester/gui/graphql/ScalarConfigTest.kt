package no.nav.melosys.tjenester.gui.graphql

import graphql.schema.CoercingSerializeException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ScalarConfigTest {

    @Test
    fun `dateCoercing skal parse string med rett datoformat`() {
        val dateCoercing = ScalarConfig.dateCoercing()


        val serializedValue = dateCoercing.serialize("2019-08-03")


        serializedValue.run {
            shouldBeInstanceOf<String>()
            shouldBe("2019-08-03")
        }
    }

    @Test
    fun `dateCoercing skal kaste exception for string med feil datoformat`() {
        val dateCoercing = ScalarConfig.dateCoercing()


        shouldThrow<CoercingSerializeException> {
            dateCoercing.serialize("20190101")
        }
    }

    @Test
    fun `dateCoercing skal kaste exception for string med tekst`() {
        val dateCoercing = ScalarConfig.dateCoercing()


        shouldThrow<CoercingSerializeException> {
            dateCoercing.serialize("Tekst")
        }
    }
}