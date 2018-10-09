package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.DokumentType;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
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
    @Produces("application/pdf")
    public Response hentDokument(@PathParam("journalpostID") String journalpostID, @PathParam("dokumentID") String dokumentID) {
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

    @GET
    @Path("utkast/pdf/{behandlingID}/{typeID}")
    @Produces("application/pdf")
    public Response produserUtkast(@PathParam("behandlingID") long behandlingID, @PathParam("typeID") String typeID) {
        byte[] dokument;

        try {
            DokumentType dokumentType = DokumentType.forKode(typeID);
            BrevDataDto brevDataDto = new BrevDataDto();
            brevDataDto.saksbehandler = SubjectHandler.getInstance().getUserID();
            dokument = dokumentService.produserUtkast(behandlingID, dokumentType, brevDataDto);
        } catch (IkkeFunnetException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
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
    @Path("opprett/{behandlingID}/{typeID}")
    public Response produserDokument(@PathParam("behandlingID") long behandlingID, @PathParam("typeID") String typeID) {
        try {
            tilgang.sjekk(behandlingID);
            DokumentType dokumentType = DokumentType.forKode(typeID);
            BrevDataDto brevDataDto = new BrevDataDto();
            brevDataDto.saksbehandler = SubjectHandler.getInstance().getUserID();
            dokumentService.produserDokument(behandlingID, dokumentType, brevDataDto);
        } catch (IkkeFunnetException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok().build();
    }
}
