package no.nav.melosys.domain.jpa

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.util.*

class PropertiesConverterTest {
    @Test
    fun `PropertiesConverter skal kunne konvertere tomme properties til null`() {
        val converter = PropertiesConverter()
        val emptyProperties = Properties()


        val databaseValue = converter.convertToDatabaseColumn(emptyProperties)


        databaseValue shouldBe null
    }

    @Test
    fun `PropertiesConverter skal kunne konvertere null properties til null`() {
        val converter = PropertiesConverter()


        val databaseValue = converter.convertToDatabaseColumn(null)


        databaseValue shouldBe null
    }

    @Test
    fun `PropertiesxConverter skal kunne konvertere null database verdi til tomme properties`() {
        val converter = PropertiesConverter()


        val properties = converter.convertToEntityAttribute(null)


        properties shouldNotBe null
        properties.isEmpty() shouldBe true
    }

    @Test
    fun `PropertiesConverter skal kunne round-trip konvertere enkle properties`() {
        val converter = PropertiesConverter()
        val originalProperties = Properties().apply {
            setProperty("key1", "value1")
            setProperty("key2", "value2")
        }


        val databaseValue = converter.convertToDatabaseColumn(originalProperties)
        val convertedProperties = converter.convertToEntityAttribute(databaseValue)


        convertedProperties shouldBe originalProperties
        convertedProperties.getProperty("key1") shouldBe "value1"
        convertedProperties.getProperty("key2") shouldBe "value2"
    }

    @Test
    fun `PropertiesConverter skal håndtere spesialtegn og unicode`() {
        val converter = PropertiesConverter()
        val originalProperties = Properties().apply {
            setProperty("norsk", "æøåÆØÅ")
            setProperty("special", "!@#$%^&*()_+-=[]{}|;':\",./<>?")
            setProperty("unicode", "🇳🇴 Unicode test ñáéíóú")
            setProperty("newlines", "line1\nline2\ttab")
        }


        val databaseValue = converter.convertToDatabaseColumn(originalProperties)
        val convertedProperties = converter.convertToEntityAttribute(databaseValue)


        convertedProperties.run {
            getProperty("norsk") shouldBe "æøåÆØÅ"
            getProperty("special") shouldBe "!@#$%^&*()_+-=[]{}|;':\",./<>?"
            getProperty("unicode") shouldBe "🇳🇴 Unicode test ñáéíóú"
            getProperty("newlines") shouldBe "line1\nline2\ttab"
        }
    }

    @Test
    fun `PropertiesConverter skal håndtere properties med tomme verdier`() {
        val converter = PropertiesConverter()
        val originalProperties = Properties().apply {
            setProperty("empty", "")
            setProperty("space", " ")
            setProperty("normal", "value")
        }


        val databaseValue = converter.convertToDatabaseColumn(originalProperties)
        val convertedProperties = converter.convertToEntityAttribute(databaseValue)


        convertedProperties.run {
            getProperty("empty") shouldBe ""
            getProperty("space") shouldBe " "
            getProperty("normal") shouldBe "value"
        }
    }

    @Test
    fun `PropertiesConverter skal håndtere store properties objekter`() {
        val converter = PropertiesConverter()
        val originalProperties = Properties()

        // Legg til mange properties for å teste ytelse og størrelse
        for (i in 1..100) {
            originalProperties.setProperty("key$i", "Dette er en lang verdi for nøkkel nummer $i med masse tekst")
        }


        val databaseValue = converter.convertToDatabaseColumn(originalProperties)
        val convertedProperties = converter.convertToEntityAttribute(databaseValue)


        convertedProperties shouldBe originalProperties
        convertedProperties.size shouldBe 100
        convertedProperties.getProperty("key1") shouldBe "Dette er en lang verdi for nøkkel nummer 1 med masse tekst"
        convertedProperties.getProperty("key100") shouldBe "Dette er en lang verdi for nøkkel nummer 100 med masse tekst"
    }
}
