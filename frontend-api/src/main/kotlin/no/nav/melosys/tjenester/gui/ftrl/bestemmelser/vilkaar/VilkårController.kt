package no.nav.melosys.tjenester.gui.ftrl.bestemmelser.vilkaar

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import mu.KotlinLogging
import no.nav.melosys.domain.jpa.konverterTilBestemmelse
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelse
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val BEHANDLINGSTEMA = "behandlingstema"
private const val BEHANDLING_ID = "behandlingID"

@Protected
@RestController
@Tags(
    Tag(name = "ftrl"),
    Tag(name = "bestemmelser"),
    Tag(name = "avklarte fakta"),
    Tag(name = "vilkår")
)
class VilkårController(
    private val vilkårForBestemmelse: VilkårForBestemmelse,
    private val aksessKontroll: Aksesskontroll
) {
    private val log = KotlinLogging.logger { }

    private val avklartefaktatyperNavn = Avklartefaktatyper.values().map { it.name }

    @GetMapping("/ftrl/bestemmelser/{bestemmelse}/vilkaar")
    fun hentVilkår(
        @PathVariable bestemmelse: String,
        @RequestParam requestParams: Map<String, String>
    ): ResponseEntity<VilkårForBestemmelseDto> {
        validerRequestParams(requestParams)
        val behandlingID = requestParams[BEHANDLING_ID]?.toLong()
        if (behandlingID != null) {
            aksessKontroll.autoriser(behandlingID)
        }

        val behandlingstema = requestParams[BEHANDLINGSTEMA] ?: throw FunksjonellException("?behandlingstema er påkrevd")

        val avklarteFakta = requestParams.filterKeys { k -> k in avklartefaktatyperNavn }
            .mapKeys { (k, _) -> Avklartefaktatyper.valueOf(k) }
        val vilkårDtoList = vilkårForBestemmelse.hentVilkår(
            konverterTilBestemmelse(bestemmelse),
            Behandlingstema.valueOf(behandlingstema),
            avklarteFakta,
            behandlingID
        ).map { VilkårOgBegrunnelserDto(it.vilkår, it.defaultOppfylt, it.muligeBegrunnelser) }

        log.debug { "FTRL vilkår for bestemmelse: $bestemmelse, $requestParams: $vilkårDtoList" }
        return ResponseEntity.ok(VilkårForBestemmelseDto(vilkårDtoList))
    }

    private fun validerRequestParams(queryParams: Map<String, String>) {
        val validKeys = listOf(BEHANDLING_ID, BEHANDLINGSTEMA) + avklartefaktatyperNavn

        val unknownKeys = queryParams.keys.filterNot { key -> validKeys.any { it.equals(key, ignoreCase = true) } }
        if (unknownKeys.isNotEmpty()) {
            throw FunksjonellException("Følgende request params støttes ikke: " + unknownKeys)
        }
    }

    data class VilkårForBestemmelseDto(val vilkår: List<VilkårOgBegrunnelserDto>)

    data class VilkårOgBegrunnelserDto(val vilkår: Vilkaar, val defaultOppfylt: Boolean?, val muligeBegrunnelser: Collection<String>)

}
