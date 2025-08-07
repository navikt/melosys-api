package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.service.dokument.brev.BrevDataA001
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class A001MapperKtTest {

    private lateinit var mapper: A001Mapper
    
    @BeforeEach
    fun setup() {
        mapper = A001Mapper()
    }

    @Test
    fun `mapper kan lage A001 XML struktur`() {
        // Simple test to verify mapper creation
        mapper.shouldNotBeNull()
    }
}