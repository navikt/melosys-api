package no.nav.melosys.domain.eessi.sed

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test

internal class SedGrunnlagDtoTest {

    private val objectMapper = ObjectMapper()

    @Test
    @Throws(JsonProcessingException::class)
    fun serialiserSedDataDto() {
        val sedDataDto = SedDataDto()
        objectMapper.writeValueAsString(sedDataDto)
    }
}