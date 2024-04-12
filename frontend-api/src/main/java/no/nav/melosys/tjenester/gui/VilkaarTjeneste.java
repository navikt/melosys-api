package no.nav.melosys.tjenester.gui;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.tilgang.Ressurs;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.service.behandling.VilkaarsresultatService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/vilkaar")
@Api(tags = {"vilkår"})
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VilkaarTjeneste {

    private final VilkaarsresultatService vilkaarsresultatService;
    private final InngangsvilkaarService inngangsvilkaarService;
    private final Aksesskontroll aksesskontroll;

    public VilkaarTjeneste(VilkaarsresultatService vilkaarsresultatService,
                           InngangsvilkaarService inngangsvilkaarService,
                           Aksesskontroll aksesskontroll) {
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("{behandlingID}")
    public List<VilkaarDto> hentVilkår(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return vilkaarsresultatService.hentVilkaar(behandlingID);
    }

    @PostMapping("{behandlingID}")
    @ApiOperation(value = "Lagre vilkår")
    public List<VilkaarDto> registrerVilkår(@PathVariable("behandlingID") long behandlingID,
                                            @RequestBody List<VilkaarDto> vilkaarDtoer) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.VILKÅR);
        vilkaarsresultatService.registrerVilkår(behandlingID, vilkaarDtoer);
        return vilkaarsresultatService.hentVilkaar(behandlingID);
    }

    @PutMapping("{behandlingID}/inngangsvilkaar/overstyr")
    @ApiOperation(value = "Overstyr vurdering av inngangsvilkår til oppfylt")
    public ResponseEntity<Void> overstyrInngangsvilkårTilOppfylt(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriserSkrivTilRessurs(behandlingID, Ressurs.VILKÅR);
        inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(behandlingID);

        return ResponseEntity.noContent().build();
    }
}
