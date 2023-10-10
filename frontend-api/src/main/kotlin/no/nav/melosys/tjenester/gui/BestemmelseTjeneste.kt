package no.nav.melosys.tjenester.gui

import io.swagger.annotations.Api
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.medlemskapsperiode.UtledBestemmelserOgVilkår
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.medlemskapsperiode.dto.BestemmelseDto
import no.nav.melosys.tjenester.gui.medlemskapsperiode.dto.BestemmelseMedVilkårOgBegrunnelserDto
import no.nav.melosys.tjenester.gui.medlemskapsperiode.dto.FolketrygdlovenBestemmelserDto
import no.nav.melosys.tjenester.gui.medlemskapsperiode.dto.VilkårOgBegrunnelserDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@Api(tags = ["medlemavfolketrygden", "bestemmelser"])
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class BestemmelseTjeneste(private val aksesskontroll: Aksesskontroll, private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService) {

    private var utledBestemmelserOgVilkår = UtledBestemmelserOgVilkår()

    @GetMapping("/behandlinger/{behandlingID}/medlemavfolketrygden/bestemmelser")
    fun hentBestemmelse(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<BestemmelseDto> {
        aksesskontroll.autoriser(behandlingID)

        return medlemAvFolketrygdenService.finnMedlemAvFolketrygden(behandlingID).map { ResponseEntity.ok(BestemmelseDto(it.bestemmelse)) }
            .orElse(ResponseEntity.noContent().build())
    }

    @PostMapping("/behandlinger/{behandlingID}/medlemavfolketrygden/bestemmelser")
    fun lagreBestemmelse(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody bestemmelseDto: BestemmelseDto
    ): ResponseEntity<BestemmelseDto> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val medlemAvFolketrygden = medlemAvFolketrygdenService.lagreBestemmelse(behandlingID, bestemmelseDto.bestemmelse)
        return ResponseEntity.ok(BestemmelseDto(medlemAvFolketrygden.bestemmelse))
    }

    @GetMapping("/behandlinger/medlemavfolketrygden/bestemmelser/{behandlingstema}")
    fun hentBestemmelserMedVilkaarForBehandlingstema(@PathVariable("behandlingstema") behandlingstema: Behandlingstema): ResponseEntity<FolketrygdlovenBestemmelserDto> {
        val støttede = utledBestemmelserOgVilkår.hentStøttedeBestemmelserOgVilkår(behandlingstema)
            .map {
                BestemmelseMedVilkårOgBegrunnelserDto(
                    it.key,
                    it.value.map { vilkår -> VilkårOgBegrunnelserDto(vilkår, utledBestemmelserOgVilkår.hentBegrunnelserForVilkår(vilkår)) })
            }
        val ustøttede = utledBestemmelserOgVilkår.hentIkkeStøttedeBestemmelserOgVilkår(behandlingstema).keys

        return ResponseEntity.ok(FolketrygdlovenBestemmelserDto(støttede, ustøttede))
    }
}
