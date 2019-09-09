package no.nav.melosys.tjenester.gui;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.dokument.DokumentService;
import no.nav.melosys.service.dokument.DokumentVisningService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.dokument.JournalpostInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"dokumenter"})
@Path("/dokumenter")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DokumentTjeneste extends RestTjeneste {
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON + "; charset=UTF-8";

    private final DokumentService dokumentService;
    private final DokumentVisningService dokumentVisningService;
    private final EessiService eessiService;
    private final TilgangService tilgangService;

    @Autowired
    public DokumentTjeneste(DokumentService dokumentService,
                            DokumentVisningService dokumentVisningService,
                            EessiService eessiService,
                            TilgangService tilgangService) {
        this.dokumentService = dokumentService;
        this.dokumentVisningService = dokumentVisningService;
        this.eessiService = eessiService;
        this.tilgangService = tilgangService;
    }

    @GET
    @Path("pdf/{journalpostID}/{dokumentID}")
    @ApiOperation(value = "hent dokument knyttet til journalpost", response = byte[].class)
    @Produces({APPLICATION_PDF, APPLICATION_JSON_UTF8})
    public Response hentDokument(@ApiParam @PathParam("journalpostID") String journalpostID, @ApiParam @PathParam("dokumentID") String dokumentID)
        throws SikkerhetsbegrensningException, IkkeFunnetException {
        byte[] dokument;
        dokument = dokumentVisningService.hentDokument(journalpostID, dokumentID);
        return lagResponseAvDokument(dokument, String.format("journalpost-dok-%s.pdf", dokumentID));
    }

    @GET
    @Path("/oversikt/{saksnummer}")
    @ApiOperation(value = "Henter alle dokumenter knyttet til en fagsak", response = JournalpostInfoDto.class, responseContainer = "List")
    public Response hentDokumenter(@PathParam("saksnummer") String saksnummer) throws IkkeFunnetException, IntegrasjonException, SikkerhetsbegrensningException {
        List<JournalpostInfoDto> dokumentListe = dokumentVisningService.hentDokumenter(saksnummer)
            .stream()
            .map(JournalpostInfoDto::av)
            .sorted(Comparator.comparing(JournalpostInfoDto::hentGjeldendeTidspunkt, Comparator.nullsFirst(Comparator.reverseOrder())))
            .collect(Collectors.toList());
        return Response.ok(dokumentListe).build();
    }

    @POST
    @Path("pdf/brev/utkast/{behandlingID}/{produserbartDokument}")
    @Produces({APPLICATION_PDF, APPLICATION_JSON_UTF8})
    public Response produserUtkastBrev(@PathParam("behandlingID") long behandlingID,
                                       @PathParam("produserbartDokument") Produserbaredokumenter produserbartDokument,
                                       BrevbestillingDto brevBestillingDto) throws TekniskException, FunksjonellException {
        byte[] dokument;
        tilgangService.sjekkTilgang(behandlingID);
        dokument = dokumentService.produserUtkast(behandlingID, produserbartDokument, brevBestillingDto);
        return lagResponseAvDokument(dokument, produserbartDokument.getKode() + "_utkast.pdf");
    }

    @GET
    @Path("pdf/sed/utkast/{behandlingID}/{sedType}")
    @Produces({APPLICATION_PDF, APPLICATION_JSON_UTF8})
    public Response produserUtkastSed(@PathParam("behandlingID") long behandlingID,
                                      @PathParam("sedType") SedType sedType) throws MelosysException {

        tilgangService.sjekkTilgang(behandlingID);
        byte[] dokument = eessiService.genererSedForhåndsvisning(behandlingID, sedType);
        return lagResponseAvDokument(dokument, sedType.name() + "_utkast.pdf");
    }

    @POST
    @Path("opprett/{behandlingID}/{produserbartDokument}")
    public Response produserDokument(@Context UriInfo uriInfo,
                                     @PathParam("behandlingID") long behandlingID,
                                     @PathParam("produserbartDokument") Produserbaredokumenter produserbartDokument,
                                     BrevbestillingDto brevBestillingDto) throws FunksjonellException, TekniskException {
        if (brevBestillingDto.mottaker == null) {
            throw new FunksjonellException("Mottaker trengs for å bestille.");
        }
        tilgangService.sjekkTilgang(behandlingID);
        dokumentService.produserDokumentISaksflyt(produserbartDokument, brevBestillingDto.mottaker, behandlingID, new BrevData(brevBestillingDto));
        return Response.noContent().build();
    }

    private static Response lagResponseAvDokument(byte[] dokument, String filnavn) {
        Response.ResponseBuilder ok = Response.ok(dokument);
        ok.header(HttpHeaders.CONTENT_LENGTH, dokument.length);
        ok.header(HttpHeaders.CONTENT_DISPOSITION, "inline; attachment; filename=" + filnavn);
        return ok.build();
    }
}
