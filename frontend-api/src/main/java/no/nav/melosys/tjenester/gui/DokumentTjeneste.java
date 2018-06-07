package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.dokument.DokumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"dokumenter"})
@Path("/dokumenter")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DokumentTjeneste extends RestTjeneste {

    private DokumentService dokumentService;

    @Autowired
    public DokumentTjeneste(DokumentService dokumentService) {
        this.dokumentService = dokumentService;
    }

    @GET
    @Path("pdf/{journalpostID}/{dokumentID}")
    @Produces("application/pdf")
    public Response hentDokument(@PathParam("journalpostID") String journalpostID, @PathParam("dokumentID") String dokumentID) {
        byte[] dokument;

        try {
            dokument = dokumentService.hentDokument(journalpostID, dokumentID);
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Response.ResponseBuilder ok = Response.ok(dokument);
        ok.header(HttpHeaders.CONTENT_LENGTH, dokument.length);
        ok.header(HttpHeaders.CONTENT_DISPOSITION, "inline");
        return ok.build();
    }

    @GET
    @Path("utkast/pdf/{behandlingID}/{typeID}")
    @Produces("application/pdf")
    public Response produserUtkast(@PathParam("behandlingID") long behandlingID, @PathParam("typeID") String typeID) {
        byte[] dokument;

        try {
            dokument = dokumentService.produserUtkast(behandlingID, typeID);
        } catch (FunksjonellException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Response.ResponseBuilder ok = Response.ok(dokument);
        ok.header(HttpHeaders.CONTENT_LENGTH, dokument.length);
        ok.header(HttpHeaders.CONTENT_DISPOSITION, "inline");
        return ok.build();
    }

    @POST
    @Path("opprett/{behandlingID}/{typeID}")
    public Response produserDokument(@PathParam("behandlingID") long behandlingID, @PathParam("typeID") String typeID) {
        try {
            dokumentService.produserDokument(behandlingID, typeID);
        } catch (FunksjonellException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.ok().build();
    }
}
