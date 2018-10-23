package no.nav.melosys.tjenester.gui;

import java.util.List;
import javax.ws.rs.*;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.vilkaar.VilkaarDto;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"vilkaar"})
@Path("/vilkaar")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VilkaarTjeneste extends RestTjeneste {

    private final VilkaarsresultatService vilkaarsresultatService;

    private final Tilgang tilgang;

    @Autowired
    public VilkaarTjeneste(VilkaarsresultatService vilkaarsresultatService, Tilgang tilgang) {
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.tilgang = tilgang;
    }

    @GET
    @Path("{behandlingID}")
    public List<VilkaarDto> hentVilkår(@PathParam("behandlingID") long behandlingID) {
        List<VilkaarDto> vilkaarDtoListe;

        try {
            tilgang.sjekk(behandlingID);
            vilkaarDtoListe = vilkaarsresultatService.hentVilkaar(behandlingID);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e);
        } catch (TekniskException e) {
            throw new InternalServerErrorException(e);
        }

        return vilkaarDtoListe;
    }

    @POST
    @Path("{behandlingID}")
    public List<VilkaarDto> registrerVilkår(@PathParam("behandlingID") long behandlingID, VilkaarDto vilkaarDto) {
        List<VilkaarDto> vilkaarDtoListe;
        try {
            tilgang.sjekk(behandlingID);
            vilkaarsresultatService.registrerVilkår(behandlingID, vilkaarDto);
            vilkaarDtoListe = vilkaarsresultatService.hentVilkaar(behandlingID);
        } catch (IkkeFunnetException e) {
            throw new NotFoundException(e);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e);
        } catch (TekniskException e) {
            throw new InternalServerErrorException(e);
        }
        
        return vilkaarDtoListe;
    }
}
