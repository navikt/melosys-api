package no.nav.melosys.sikkerhet.abac

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.freg.abac.core.annotation.context.AbacContext
import no.nav.freg.abac.core.dto.request.XacmlRequest
import no.nav.freg.abac.core.dto.response.Decision
import no.nav.freg.abac.core.dto.response.XacmlResponse
import no.nav.freg.abac.core.service.AbacService
import no.nav.melosys.exception.SikkerhetsbegrensningException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PepTest {

    @InjectMockKs
    lateinit var pep: PepImpl

    @MockK
    private lateinit var abacService: AbacService

    @MockK
    private lateinit var abacContext: AbacContext

    @MockK(relaxed = true)
    private lateinit var abacResponse: XacmlResponse

    @BeforeEach
    fun setUp() {
        every { abacContext.request } returns XacmlRequest()
        every { abacService.evaluate(any()) } returns abacResponse
    }

    @Test
    fun `sjekk tilgang til fnr med PERMIT skal ikke kaste exception`() {
        every { abacResponse.decision } returns Decision.PERMIT
        pep.sjekkTilgangTilFnr("12345678910")
    }

    @Test
    fun `sjekk tilgang til fnr med DENY skal kaste SikkerhetsbegrensningException`() {
        every { abacResponse.decision } returns Decision.DENY
        val exception = shouldThrow<SikkerhetsbegrensningException> {
            pep.sjekkTilgangTilFnr("12345678910")
        }
        exception.message shouldContain "ikke tilgang"
    }

    @Test
    fun `sjekk tilgang til fnr med INDETERMINATE skal kaste SikkerhetsbegrensningException`() {
        every { abacResponse.decision } returns Decision.INDETERMINATE
        val exception = shouldThrow<SikkerhetsbegrensningException> {
            pep.sjekkTilgangTilFnr("12345678910")
        }
        exception.message shouldContain "ikke tilgang"
    }

    @Test
    fun `sjekk tilgang til fnr med NOT_APPLICABLE skal kaste SikkerhetsbegrensningException`() {
        every { abacResponse.decision } returns Decision.NOT_APPLICABLE
        val exception = shouldThrow<SikkerhetsbegrensningException> {
            pep.sjekkTilgangTilFnr("12345678910")
        }
        exception.message shouldContain "ikke tilgang"
    }

    @Test
    fun `sjekk tilgang til aktor med PERMIT skal ikke kaste exception`() {
        every { abacResponse.decision } returns Decision.PERMIT
        pep.sjekkTilgangTilAktoerId("12345678910")
    }

    @Test
    fun `sjekk tilgang til aktor med DENY skal kaste SikkerhetsbegrensningException`() {
        every { abacResponse.decision } returns Decision.DENY
        val exception = shouldThrow<SikkerhetsbegrensningException> {
            pep.sjekkTilgangTilFnr("12345678910")
        }
        exception.message shouldContain "ikke tilgang"
    }

    @Test
    fun `sjekk tilgang til aktor med INDETERMINATE skal kaste SikkerhetsbegrensningException`() {
        every { abacResponse.decision } returns Decision.INDETERMINATE
        val exception = shouldThrow<SikkerhetsbegrensningException> {
            pep.sjekkTilgangTilFnr("12345678910")
        }
        exception.message shouldContain "ikke tilgang"
    }

    @Test
    fun `sjekk tilgang til aktor med NOT_APPLICABLE skal kaste SikkerhetsbegrensningException`() {
        every { abacResponse.decision } returns Decision.NOT_APPLICABLE
        val exception = shouldThrow<SikkerhetsbegrensningException> {
            pep.sjekkTilgangTilFnr("12345678910")
        }
        exception.message shouldContain "ikke tilgang"
    }
}
