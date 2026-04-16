package no.nav.melosys

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.module.kotlin.jacksonObjectMapper

fun Any.toJsonNode(objectMapper: ObjectMapper): JsonNode = objectMapper.valueToTree(this)

fun Any.toJsonString(objectMapper: ObjectMapper): String = this.toJsonNode(objectMapper).toPrettyString()

/*
    * This function is used to convert an object to a JsonNode for debugging purposes.
    * We should use mapper from spring context to ensure that the object is serialized correctly when possible.
 */
fun Any.toJsonNodeForDebugging(): JsonNode = this.toJsonNode(jacksonObjectMapper().apply {
})
