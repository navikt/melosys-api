package no.nav.melosys

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun Any.toJsonNode(objectMapper: ObjectMapper): JsonNode = objectMapper.valueToTree(this)

fun Any.toJsonString(objectMapper: ObjectMapper): String = this.toJsonNode(objectMapper).toPrettyString()

/*
    * This function is used to convert an object to a JsonNode for debugging purposes.
    * We should use mapper from spring context to ensure that the object is serialized correctly when possible.
 */
fun Any.toJsonNodeForDebugging(): JsonNode = this.toJsonNode(jacksonObjectMapper().apply {
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    registerModule(JavaTimeModule())
})
