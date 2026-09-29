package no.nav.melosys.tjenester.gui.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

private val log = KotlinLogging.logger { }

@Component
class AdminTilgangInterceptor(
    @Value("\${Melosys-admin.driftsgruppe}") private val driftsgruppeId: String,
    // Kommaseparert i local-mock (to mock-verdier), én klient-ID i Nais
    @Value("\${Melosys-admin.console-klient-id}") private val consoleKlientIder: List<String>
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val subjectHandler = SubjectHandler.getInstance()

        if (manglerGyldigToken(subjectHandler)) return true   // @Protected svarer 401

        // TODO MELOSYS-8271 (rød sone): avvis med 403 og UKJENT_KLIENT når tokenets azp ikke er
        //  en av consoleKlientIder. Gjelder både person- og maskinkall, så sjekken må komme før
        //  maskinkall slippes gjennom. Logg årsak og avvist azp (klient-ID), aldri token eller NAV-ident.

        if (erMaskinkall(subjectHandler)) return true
        if (erMedlemAvDriftsgruppe(subjectHandler)) return true

        log.warn { "Admin-kall avvist: personkall uten driftsgruppe (${request.method})" }
        response.status = 403
        response.writer.write(MANGLER_DRIFTSGRUPPE)
        return false
    }

    // Gjelder alle tokens: OBO-token (personkall via Console) og M2M-token (maskinkall, idtyp = "app")
    private fun manglerGyldigToken(subjectHandler: SubjectHandler) = subjectHandler.oidcTokenString == null

    private fun erMaskinkall(subjectHandler: SubjectHandler) = subjectHandler.tokenIdType == IDTYP_MASKIN

    private fun erMedlemAvDriftsgruppe(subjectHandler: SubjectHandler) = driftsgruppeId in subjectHandler.groups

    companion object {
        const val MANGLER_DRIFTSGRUPPE = "Mangler tilgang til admin-endepunkter"
        const val UKJENT_KLIENT = "Kallet kommer ikke fra en godkjent klient"
        private const val IDTYP_MASKIN = "app"
    }
}
