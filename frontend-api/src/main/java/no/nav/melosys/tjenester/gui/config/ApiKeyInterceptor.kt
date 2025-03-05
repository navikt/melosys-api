package no.nav.melosys.tjenester.gui.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.melosys.exception.SikkerhetsbegrensningException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class ApiKeyInterceptor(
    @Value("\${Melosys-admin.apikey}") private val apiKey: String
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (request.getHeader(API_KEY_HEADER) != apiKey) {
            throw SikkerhetsbegrensningException("Trenger gyldig apikey")
        }
        return true
    }

    companion object {
        const val API_KEY_HEADER = "X-MELOSYS-ADMIN-APIKEY"
    }
}
