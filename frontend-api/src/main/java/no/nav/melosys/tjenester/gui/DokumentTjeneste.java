package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Dokumenttype;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"dokumenter"})
@Path("/dokumenter")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DokumentTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(DokumentTjeneste.class);
    
    private DokumentService dokumentService;

    private final Tilgang tilgang;

    @Autowired
    public DokumentTjeneste(DokumentService dokumentService, Tilgang tilgang) {
        this.dokumentService = dokumentService;
        this.tilgang = tilgang;
    }

    @GET
    @Path("pdf/{journalpostID}/{dokumentID}")
    @ApiOperation(value = "hent dokument knyttet til journalpost", response = byte[].class)
    @Produces("application/pdf")
    public Response hentDokument(@ApiParam @PathParam("journalpostID") String journalpostID, @ApiParam @PathParam("dokumentID") String dokumentID) {
        byte[] dokument;

        try {
            dokument = dokumentService.hentDokument(journalpostID, dokumentID);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e.getMessage());
        } catch (IntegrasjonException e) {
            log.error("IntegrasjonException", e);
            throw new InternalServerErrorException(e.getMessage());
        } catch (IkkeFunnetException e) {
            throw new NotFoundException(e.getMessage());
        } catch (FunksjonellException e) {
            log.error("FunksjonellException", e);
            throw new BadRequestException("Funksjonell feil: " + e.getMessage());
        }

        Response.ResponseBuilder ok = Response.ok(dokument);
        ok.header(HttpHeaders.CONTENT_LENGTH, dokument.length);
        ok.header(HttpHeaders.CONTENT_DISPOSITION, "inline");
        return ok.build();
    }

    @POST
    @Path("utkast/pdf/{behandlingID}/{dokumenttypeKode}")
    @Produces("application/pdf")
    public Response produserUtkast(@PathParam("behandlingID") long behandlingID,
                                   @PathParam("dokumenttypeKode") Dokumenttype dokumenttypeKode,
                                   BrevDataDto brevDataDto) {
        byte[] dokument;

        try {
            tilgang.sjekk(behandlingID);
            dokument = dokumentService.produserUtkast(behandlingID, dokumenttypeKode, brevDataDto);
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (FunksjonellException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        Response.ResponseBuilder ok = Response.ok(dokument);
        ok.header(HttpHeaders.CONTENT_LENGTH, dokument.length);
        ok.header(HttpHeaders.CONTENT_DISPOSITION, "inline");
        return ok.build();
    }

    @POST
    @Path("opprett/{behandlingID}/{dokumenttypeKode}")
    public Response produserDokument(@Context UriInfo uriInfo,
                                     @PathParam("behandlingID") long behandlingID,
                                     @PathParam("dokumenttypeKode") Dokumenttype dokumenttypeKode,
                                     BrevDataDto brevDataDto) {
        try {
            tilgang.sjekk(behandlingID);
            dokumentService.produserDokumentISaksflyt(behandlingID, dokumenttypeKode, brevDataDto);
            return Response.noContent().build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (FunksjonellException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
