package no.nav.melosys.tjenester.gui;

import java.util.Optional;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.tjenester.gui.dto.KontaktInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"fagsaker"})
@Path("/fagsaker")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class KontaktopplysningTjeneste extends RestTjeneste {
    private final KontaktopplysningService kontaktopplysningService;

    @Autowired
    public KontaktopplysningTjeneste(KontaktopplysningService kontaktopplysningService) {
        this.kontaktopplysningService = kontaktopplysningService;
    }

    @GET
    @Path("/{saksnummer}/kontaktopplysninger/{orgnr}")
    @ApiOperation(
        value = "Henter kontakt orgnummer og person navn for gitt fagsak og orgnummer",
        response = Kontaktopplysning.class)
    public Response hentKontaktopplysning(@PathParam("saksnummer") String saksnummer,
                                          @PathParam("orgnr") String orgnr) {
        Optional<Kontaktopplysning> kontaktopplysning = kontaktopplysningService.hentKontaktopplysning(saksnummer, orgnr);
        if (kontaktopplysning.isPresent()) {
            return Response.ok(kontaktopplysning.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Path("/{saksnummer}/kontaktopplysninger/{orgnr}")
    @ApiOperation(
        value = "Lagrer/oppdaterer kontakt orgnummer og navn for gitt fagsak og orgnummer",
        response = Kontaktopplysning.class)
    public Response lagKontaktopplysning(@PathParam("saksnummer") String saksnummer,
                                         @PathParam("orgnr") String orgnr,
                                         KontaktInfoDto kontaktInfoDto) {
        Kontaktopplysning kontaktopplysning = kontaktopplysningService.lagEllerOppdaterKontaktopplysning(saksnummer, orgnr,
            kontaktInfoDto.getKontaktorgnr(), kontaktInfoDto.getKontaktnavn());
        return Response.ok(kontaktopplysning).build();
    }

    @DELETE
    @Path("{saksnummer}/kontaktopplysninger/{orgnr}")
    @ApiOperation(value = "Sletter kontaktopplysning på en fagsak med gitt orgnummer")
    public Response slettKontaktopplysning(@PathParam("saksnummer") String saksnummer, @PathParam("orgnr") String orgnr) throws FunksjonellException {
        kontaktopplysningService.slettKontaktopplysning(saksnummer, orgnr);
        return Response.ok().build();
    }
}
