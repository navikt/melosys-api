package no.nav.melosys.tjenester.gui.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.test.web.servlet.ResultMatcher
import java.nio.charset.StandardCharsets

fun responseBody(objectMapper: ObjectMapper) = ResponseBodyMatchers(objectMapper)

class ResponseBodyMatchers(private val objectMapper: ObjectMapper) {

    fun <T> containsObjectAsJson(
        expectedObject: Any?,
        targetClass: Class<T>
    ): ResultMatcher = ResultMatcher { mvcResult ->
        val actualJson = mvcResult.response.getContentAsString(StandardCharsets.UTF_8)
        val expectedJson = objectMapper.writeValueAsString(expectedObject)
        val normalizedActual = objectMapper.readTree(actualJson)
        val normalizedExpected = objectMapper.readTree(expectedJson)
        normalizedActual shouldBe normalizedExpected
    }

    fun <T> containsObjectAsJson(
        expectedObject: Any?,
        valueTypeRef: TypeReference<T>
    ): ResultMatcher = ResultMatcher { mvcResult ->
        val actualJson = mvcResult.response.getContentAsString(StandardCharsets.UTF_8)
        val expectedJson = objectMapper.writeValueAsString(expectedObject)
        val normalizedActual = objectMapper.readTree(actualJson)
        val normalizedExpected = objectMapper.readTree(expectedJson)
        normalizedActual shouldBe normalizedExpected
    }

    fun containsError(
        expectedFieldName: String,
        expectedMessage: String
    ): ResultMatcher = ResultMatcher { mvcResult ->
        val json = mvcResult.response.getContentAsString(StandardCharsets.UTF_8)
        val errorMap = objectMapper.readValue(json, object : TypeReference<HashMap<String, String>>() {})
        errorMap[expectedFieldName].run {
            this shouldNotBe null
            this shouldBe expectedMessage
        }
    }
}
