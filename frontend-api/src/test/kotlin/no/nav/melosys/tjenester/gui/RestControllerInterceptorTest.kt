package no.nav.melosys.tjenester.gui

import io.kotest.matchers.shouldBe
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/**
 * MELOSYS-8271: Om et kall regnes som admin-kall (og dermed bruker systemtoken videre)
 * avgjøres av stien, ikke av den fjernede adminnøkkelen.
 */
class RestControllerInterceptorTest {

    private val interceptor = RestControllerInterceptor()
    private val response = MockHttpServletResponse()
    private val handler = Any()

    private fun brukerSystemtokenUnderKall(request: MockHttpServletRequest): Boolean {
        interceptor.preHandle(request, response, handler)
        try {
            return ThreadLocalAccessInfo.shouldUseSystemToken()
        } finally {
            interceptor.afterCompletion(request, response, handler, null)
        }
    }

    @Test
    fun `admin-kall uten nøkkelheader skal bruke systemtoken`() {
        val request = MockHttpServletRequest("GET", "/admin/prosessinstanser/feilede")

        brukerSystemtokenUnderKall(request) shouldBe true
    }

    @Test
    fun `vanlig api-kall skal ikke bruke systemtoken`() {
        val request = MockHttpServletRequest("GET", "/api/fagsaker/MEL-1")

        brukerSystemtokenUnderKall(request) shouldBe false
    }

    @Test
    fun `nøkkelheader skal ikke gjøre et vanlig api-kall til admin-kall`() {
        val request = MockHttpServletRequest("GET", "/api/fagsaker/MEL-1").apply {
            addHeader("X-MELOSYS-ADMIN-APIKEY", "dummy")
        }

        brukerSystemtokenUnderKall(request) shouldBe false
    }

    @Test
    fun `admin-sti under api-prefikset skal ikke regnes som admin-kall`() {
        // TekstblokkAdminController ligger under /api/admin og går ikke gjennom AdminTilgangInterceptor
        val request = MockHttpServletRequest("POST", "/api/admin/brev/tekstblokker/bulk")

        brukerSystemtokenUnderKall(request) shouldBe false
    }
}
