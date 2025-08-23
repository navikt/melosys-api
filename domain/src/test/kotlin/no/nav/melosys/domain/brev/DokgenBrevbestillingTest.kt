package no.nav.melosys.domain.brev

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Mottakerroller
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

/**
 * Sjekker at alle subtyper av DokgenBrevbestilling kan deserialiseres til riktig type.
 * Eksempel: Dersom alle feltene til subklasse X også inneholder i subklasse Z, vil deserialiseringen feile.
 */
class DokgenBrevbestillingTest {

    private val dataMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(SimpleModule().addDeserializer(LovvalgBestemmelse::class.java, LovvalgBestemmelseDeserializer()))

    @Test
    fun `deserialisering skal bli riktig type for alle subtyper av DokgenBrevbestilling`() {
        val jsonSubTypes = DokgenBrevbestilling::class.java.getAnnotation(JsonSubTypes::class.java)
            ?: throw IllegalStateException("DokgenBrevbestilling mangler JsonSubTypes-annotasjon")

        val classes = jsonSubTypes.value.map { it.value.java }.toList()

        for (type in classes) {
            val node = getAllNodesFromType(type)
            val dokgenBrevbestilling: DokgenBrevbestilling? = dataMapper.readValue(node.toPrettyString(), DokgenBrevbestilling::class.java)
            println("${node.toPrettyString()} -> ${type.simpleName}") // beholder denne siden den viser tydligere hva som sjekkes
            dokgenBrevbestilling.shouldNotBeNull()
                .shouldBeInstanceOf<DokgenBrevbestilling>()
                .javaClass shouldBe type
        }
    }

    private fun getAllNodesFromType(type: Class<*>): ObjectNode = dataMapper.createObjectNode().also { node ->
        type.declaredFields.forEach { field ->
            when (field.type.simpleName) {
                "boolean", "Boolean" -> node.put(field.name, false)
                "LocalDate" -> node.put(field.name, "2022-02-02")
                "Periode" -> node.set<ObjectNode>(field.name, dataMapper.createObjectNode().apply {
                    put("fom", "2022-02-02")
                    put("tom", "2022-02-02")
                })

                Mottakerroller::class.java.simpleName -> node.put(field.name, Mottakerroller.NORSK_MYNDIGHET.name)
                Ikkeyrkesaktivsituasjontype::class.java.simpleName -> node.put(field.name, Ikkeyrkesaktivsituasjontype.ANNET.name)
                Betalingsstatus::class.java.simpleName -> node.put(field.name, Betalingsstatus.DELVIS_BETALT.name)
                "List" -> node.set<ArrayNode>(field.name, ArrayNode(dataMapper.nodeFactory).apply {
                    add("Norge")
                    add("Sverige")
                })

                else -> node.put(field.name, field.type.simpleName)
            }
        }
    }
}
