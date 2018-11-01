package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.LovvalgsperiodeDto;

@Api(tags = { "lovvalgsperiode" })
@Service
@Path("/lovvalgsperioder")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class LovvalgsperiodeTjeneste extends RestTjeneste {

    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final Tilgang tilgang;

    public LovvalgsperiodeTjeneste(LovvalgsperiodeService lovvalgsperiodeService, Tilgang tilgang) {
        super();
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.tilgang = tilgang;
    }

    @GET
    @Path("{behandlingsid}")
    @ApiOperation(value = "Henter en lovvalgsperiode for en gitt behandling", response = LovvalgsperiodeDto.class)
    public Response hentLovvalgsperiode(@PathParam("behandlingsid") long behandlingsid) {
        sjekkTilgang(behandlingsid);
        Collection<LovvalgsperiodeDto> resultat = lovvalgsperiodeService
                .hentLovvalgsperioder(behandlingsid)
                .stream()
                .map(LovvalgsperiodeDto::av)
                .collect(Collectors.toList());
        return Response.ok(resultat).build();
    }

    @POST
    @Path("{behandlingsid}")
    @ApiOperation("Lagrer en lovvalgsperiode for en gitt behandling.")
    public Collection<LovvalgsperiodeDto> lagreLovvalgsperiode(@PathParam("behandlingsid") long behandlingsid,
            @ApiParam(value = "En liste av lovvalgsperioder å lagre.") Collection<LovvalgsperiodeDto> lovvalgsperiodeDtoer) {
        sjekkTilgang(behandlingsid);
        List<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeDtoer.stream()
                .map(LovvalgsperiodeDto::til)
                .collect(Collectors.toList());
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingsid, lovvalgsperioder);
        return lovvalgsperiodeDtoer;
    }

    private final void sjekkTilgang(long behandlingsid) {
        try {
            tilgang.sjekk(behandlingsid);
        } catch (SikkerhetsbegrensningException e) {
            // TBD: Bør dette logges?
            throw new ForbiddenException(e);
        } catch (TekniskException e) {
            // TBD: Blir dette logget av Jersey?
            throw new InternalServerErrorException(e);
        }
    }

}
