package no.nav.melosys.service.vedtak

import io.swagger.v3.oas.annotations.Operation
import mu.KotlinLogging
import no.nav.melosys.service.vedtak.IkkeYrkesaktivVedtaksmetadataFiksService.Companion.MAKS_ANTALL_RADER
import no.nav.melosys.service.vedtak.IkkeYrkesaktivVedtaksmetadataFiksService.Companion.STANDARD_MAKS_ANTALL_RADER
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@Protected
@RestController
@RequestMapping("/admin/vedtaksmetadata-fiks/ikke-yrkesaktive")
class IkkeYrkesaktivVedtaksmetadataFiksController(
    private val fiksService: IkkeYrkesaktivVedtaksmetadataFiksService
) {

    @Operation(
        summary = "Legg inn manglende vedtaksmetadata for ikke-yrkesaktive vedtak (datafiks MELOSYS-8351)",
        description = "Vedtak fattet i flyten for ikke-yrkesaktive i EØS og trygdeavtale før MELOSYS-8337 mangler " +
            "rad i vedtak_metadata. Kjør ikke skarpt før MELOSYS-8337 er i prod, og kjør ett kall om gangen. " +
            "Med skarp=false (standard) endres ingenting: svaret viser antall per gruppe, " +
            "sakstype og behandlingstype, og vedtakene som holdes utenfor. Med skarp=true legges høyst " +
            "maksAntallRader rader inn (standard $STANDARD_MAKS_ANTALL_RADER, høyst $MAKS_ANTALL_RADER), eldste " +
            "først. antallIgjen i svaret sier hvor mange som gjenstår; gjenta kallet til den er 0. " +
            "Vedtaksdatoen er behandlingsresultatets endret_dato, klagefristen 42 dager etter, og vedtakstypen " +
            "FØRSTEGANGSVEDTAK for førstegangsbehandling og ENDRINGSVEDTAK for ny vurdering. Radene merkes " +
            "${IkkeYrkesaktivVedtaksmetadataFiksService.PATCH_MARKØR} og kan slettes med /angre."
    )
    @PostMapping
    fun fiks(@RequestBody(required = false) request: IkkeYrkesaktivFiksRequest?): ResponseEntity<Any> {
        val fiksRequest = request ?: IkkeYrkesaktivFiksRequest()
        if (fiksRequest.maksAntallRader !in 1..MAKS_ANTALL_RADER) {
            return ResponseEntity.badRequest().body(
                mapOf("feil" to "maksAntallRader må være mellom 1 og $MAKS_ANTALL_RADER", "maksAntallRader" to fiksRequest.maksAntallRader)
            )
        }

        log.info {
            "Datafiks vedtaksmetadata ikke-yrkesaktive (${if (fiksRequest.skarp) "SKARP" else "FORHÅNDSVISNING"}), " +
                "maksAntallRader=${fiksRequest.maksAntallRader}"
        }

        return ResponseEntity.ok(
            if (fiksRequest.skarp) fiksService.utfør(fiksRequest.maksAntallRader) else fiksService.forhåndsvis()
        )
    }

    @Operation(
        summary = "Slett radene datafiksen for MELOSYS-8351 har lagt inn",
        description = "Med skarp=false (standard) endres ingenting: svaret viser hvor mange rader som ville blitt " +
            "slettet. Med skarp=true slettes alle rader der både registrert_av og endret_av er " +
            "${IkkeYrkesaktivVedtaksmetadataFiksService.PATCH_MARKØR}. Rader som er endret etter datafiksen, " +
            "slettes aldri; de telles i antallSomIkkeKanAngres."
    )
    @PostMapping("/angre")
    fun angre(@RequestBody(required = false) request: IkkeYrkesaktivAngreRequest?): ResponseEntity<Any> {
        val skarp = request?.skarp ?: false
        log.info { "Angre datafiks vedtaksmetadata ikke-yrkesaktive (${if (skarp) "SKARP" else "FORHÅNDSVISNING"})" }
        return ResponseEntity.ok(fiksService.angre(skarp))
    }
}

data class IkkeYrkesaktivFiksRequest(
    val skarp: Boolean = false,
    val maksAntallRader: Int = STANDARD_MAKS_ANTALL_RADER,
)

data class IkkeYrkesaktivAngreRequest(
    val skarp: Boolean = false,
)
