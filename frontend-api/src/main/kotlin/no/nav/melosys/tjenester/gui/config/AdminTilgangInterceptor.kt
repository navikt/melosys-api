package no.nav.melosys.tjenester.gui.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping

private val log = KotlinLogging.logger { }
@Component
class AdminTilgangInterceptor (
    @Value("\${Melosys-admin.driftsgruppe}") private val driftsgruppe: String
): HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        //Lar @Protected håndtere tilfeller hvor det ikke finnes token, eller token er ugyldig.
        //@Protected vil returnere 401 dersom token ikke finnes eller er ugyldig.
        SubjectHandler.getInstance().oidcTokenString?: return true

        if (SubjectHandler.getInstance().tokenIdType == "app") return true

        if (!hasAccessGroup()) {
            // Rutemønster ({saksnummer} osv.), ikke requestURI, så ID-er ikke havner i loggen
            val rute = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
            log.warn { "Admin-kall avvist: personkall uten driftsgruppe (${request.method} $rute)" }
            response.status = 403
            response.writer.write("Mangler tilgang til admin-endepunkter")
            return false
        }
        return true
    }

    private fun hasAccessGroup() : Boolean{
        return SubjectHandler.getInstance().groups.contains(driftsgruppe)
    }
}
