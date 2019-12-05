package no.nav.melosys.tjenester.gui;

import java.util.List;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"vilkår"})
@RestController("/vilkaar")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VilkaarTjeneste extends RestTjeneste {

    private final VilkaarsresultatService vilkaarsresultatService;

    private final TilgangService tilgangService;

    @Autowired
    public VilkaarTjeneste(VilkaarsresultatService vilkaarsresultatService, TilgangService tilgangService) {
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("{behandlingID}")
    public List<VilkaarDto> hentVilkår(@PathVariable("behandlingID") long behandlingID) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        List<VilkaarDto> vilkaarDtoListe;

        tilgangService.sjekkTilgang(behandlingID);
        vilkaarDtoListe = vilkaarsresultatService.hentVilkaar(behandlingID);

        return vilkaarDtoListe;
    }

    @PostMapping("{behandlingID}")
    @ApiOperation(value = "Lagre vilkår")
    public List<VilkaarDto> registrerVilkår(@PathVariable("behandlingID") long behandlingID,
            @ApiParam("VilkaarData") List<VilkaarDto> vilkaarDtoer) throws FunksjonellException, TekniskException {
        List<VilkaarDto> vilkaarDtoListe;
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        vilkaarsresultatService.registrerVilkår(behandlingID, vilkaarDtoer);
        vilkaarDtoListe = vilkaarsresultatService.hentVilkaar(behandlingID);

        return vilkaarDtoListe;
    }
}
