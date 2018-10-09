package no.nav.melosys.tjenester.gui;

import java.net.URI;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.DokumentType;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.exception.*;
import no.nav.melosys.integrasjon.doksys.DokumentbestillingResponse;
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
    @Path("utkast/pdf/{behandlingID}/{dokumentTypeID}")
    @Produces("application/pdf")
    public Response produserUtkast(@PathParam("behandlingID") long behandlingID,
                                   @PathParam("dokumentTypeID") String dokumentTypeID,
                                   BrevDataDto brevDataDto) {
        byte[] dokument;

        try {
            tilgang.sjekk(behandlingID);
            DokumentType dokumentType = DokumentType.forKode(dokumentTypeID);
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
    @Path("opprett/{behandlingID}/{dokumentTypeID}")
    public Response produserDokument(@PathParam("behandlingID") long behandlingID,
                                     @PathParam("dokumentTypeID") String dokumentTypeID,
                                     BrevDataDto brevDataDto) {
        try {
            tilgang.sjekk(behandlingID);
            DokumentType dokumentType = DokumentType.forKode(dokumentTypeID);
            dokumentService.produserDokument(behandlingID, dokumentType, brevDataDto);
            DokumentbestillingResponse response = dokumentService.produserDokument(behandlingID, dokumentType, brevDataDto);
            String location = String.join("/", "dokumenter/pdf/", response.journalpostId, response.dokumentId);
            return Response.created(URI.create(location)).build();
        } catch (IkkeFunnetException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
