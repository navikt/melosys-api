package no.nav.melosys.tjenester.gui;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.service.tilgang.TilgangService;
import no.nav.melosys.service.vilkaar.InngangsvilkaarService;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final TilgangService tilgangService;

    @Autowired
    public VilkaarTjeneste(VilkaarsresultatService vilkaarsresultatService,
                           InngangsvilkaarService inngangsvilkaarService,
                           TilgangService tilgangService) {
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.inngangsvilkaarService = inngangsvilkaarService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("{behandlingID}")
    public List<VilkaarDto> hentVilkår(@PathVariable("behandlingID") long behandlingID) {
        List<VilkaarDto> vilkaarDtoListe;

        tilgangService.sjekkTilgang(behandlingID);
        vilkaarDtoListe = vilkaarsresultatService.hentVilkaar(behandlingID);

        return vilkaarDtoListe;
    }

    @PostMapping("{behandlingID}")
    @ApiOperation(value = "Lagre vilkår")
    public List<VilkaarDto> registrerVilkår(@PathVariable("behandlingID") long behandlingID,
            @RequestBody List<VilkaarDto> vilkaarDtoer) {
        List<VilkaarDto> vilkaarDtoListe;
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        vilkaarsresultatService.registrerVilkår(behandlingID, vilkaarDtoer);
        vilkaarDtoListe = vilkaarsresultatService.hentVilkaar(behandlingID);

        return vilkaarDtoListe;
    }

    @PutMapping("{behandlingID}/inngangsvilkaar/overstyr")
    @ApiOperation(value = "Overstyr vurdering av inngangsvilkår til oppfylt")
    public ResponseEntity<Void> overstyrInngangsvilkårTilOppfylt(@PathVariable("behandlingID") long behandlingID) {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(behandlingID);

        return ResponseEntity.ok().build();
    }
}
