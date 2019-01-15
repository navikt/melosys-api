package no.nav.melosys.tjenester.gui;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.kodeverk.ProduserbartDokument;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.tjenester.gui.dto.dokument.JournalpostInfoDto;
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
    @Produces({ "application/pdf", MediaType.APPLICATION_JSON + "; charset=UTF-8" })
    public Response hentDokument(@ApiParam @PathParam("journalpostID") String journalpostID, @ApiParam @PathParam("dokumentID") String dokumentID)
            throws SikkerhetsbegrensningException, IntegrasjonException, IkkeFunnetException {
        byte[] dokument;
        dokument = dokumentService.hentDokument(journalpostID, dokumentID);
        Response.ResponseBuilder ok = Response.ok(dokument);
        ok.header(HttpHeaders.CONTENT_LENGTH, dokument.length);
        ok.header(HttpHeaders.CONTENT_DISPOSITION, "inline");
        return ok.build();
    }

    @GET
    @Path("/oversikt/{saksnummer}")
    @ApiOperation(value = "Henter alle dokumenter knyttet til en fagsak", response = JournalpostInfoDto.class, responseContainer = "List")
    public Response hentDokumenter(@PathParam("saksnummer") String saksnummer) throws IkkeFunnetException, IntegrasjonException, SikkerhetsbegrensningException {
        List<JournalpostInfoDto> dokumentListe = dokumentService.hentDokumenter(saksnummer)
            .stream()
            .map(JournalpostInfoDto::av)
            .collect(Collectors.toList());
        dokumentListe.sort(Comparator.comparing(JournalpostInfoDto::hentGjeldendeTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())));
        return Response.ok(dokumentListe).build();
    }

    @POST
    @Path("utkast/pdf/{behandlingID}/{produserbartDokument}")
    @Produces({ "application/pdf", MediaType.APPLICATION_JSON + "; charset=UTF-8" })
    public Response produserUtkast(@PathParam("behandlingID") long behandlingID,
                                   @PathParam("produserbartDokument") ProduserbartDokument produserbartDokument,
            BrevbestillingDto brevBestillingDto) throws TekniskException, FunksjonellException {
        byte[] dokument;
        tilgang.sjekk(behandlingID);
        dokument = dokumentService.produserUtkast(behandlingID, produserbartDokument, brevBestillingDto);
        Response.ResponseBuilder ok = Response.ok(dokument);
        ok.header(HttpHeaders.CONTENT_LENGTH, dokument.length);
        ok.header(HttpHeaders.CONTENT_DISPOSITION, "inline");
        return ok.build();
    }

    @POST
    @Path("opprett/{behandlingID}/{produserbartDokument}")
    public Response produserDokument(@Context UriInfo uriInfo,
                                     @PathParam("behandlingID") long behandlingID,
                                     @PathParam("produserbartDokument") ProduserbartDokument produserbartDokument,
            BrevbestillingDto brevBestillingDto) throws FunksjonellException, TekniskException {
        tilgang.sjekk(behandlingID);
        dokumentService.produserDokumentISaksflyt(behandlingID, produserbartDokument, brevBestillingDto);
        return Response.noContent().build();
    }

}
