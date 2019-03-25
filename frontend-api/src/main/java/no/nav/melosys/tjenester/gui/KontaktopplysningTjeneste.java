package no.nav.melosys.tjenester.gui;

import java.util.Optional;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Kontaktopplysning;
import no.nav.melosys.domain.KontaktopplysningID;
import no.nav.melosys.repository.KontaktopplysningRepository;
import no.nav.melosys.tjenester.gui.dto.KontaktInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"fagsaker"})
@Path("/fagsaker")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class KontaktopplysningTjeneste extends RestTjeneste {
    private final KontaktopplysningRepository kontaktopplysningRepo;

    @Autowired
    public KontaktopplysningTjeneste(KontaktopplysningRepository kontaktopplysningRepo) {
        this.kontaktopplysningRepo = kontaktopplysningRepo;
    }

    @GET
    @Path("/{saksnummer}/kontaktopplysninger/{orgnr}")
    @ApiOperation(
        value = "Henter kontakt orgnummer og person navn for gitt fagsak og orgnummer",
        response = Kontaktopplysning.class)
    public Response hentKontaktopplysning(@PathParam("saksnummer") String saksnummer,
                                          @PathParam("orgnr") String orgnr) {
        Optional<Kontaktopplysning> kontaktopplysning = kontaktopplysningRepo.findById(new KontaktopplysningID(saksnummer, orgnr));
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
        Kontaktopplysning kontaktopplysning = kontaktopplysningRepo.findById(new KontaktopplysningID(saksnummer, orgnr))
            .orElseGet(() -> {
                Kontaktopplysning lokalKontaktopplysning = new Kontaktopplysning();
                lokalKontaktopplysning.setKontaktopplysningID(new KontaktopplysningID(saksnummer, orgnr));
                return lokalKontaktopplysning;
            });
        kontaktopplysning.setKontaktOrgnr(kontaktInfoDto.kontaktorgnr);
        kontaktopplysning.setKontaktNavn(kontaktInfoDto.kontaktnavn);
        kontaktopplysningRepo.save(kontaktopplysning);

        return Response.ok(kontaktopplysning).build();
    }

    @DELETE
    @Path("{saksnummer}/kontaktopplysninger/{orgnr}")
    @ApiOperation(value = "Sletter kontaktopplysning på en fagsak med gitt orgnummer")
    public Response slettKontaktopplysning(@PathParam("saksnummer") String saksnummer, @PathParam("orgnr") String orgnr) {
        try {
            kontaktopplysningRepo.deleteById(new KontaktopplysningID(saksnummer, orgnr));
        } catch (EmptyResultDataAccessException e) {
            throw new BadRequestException("Finner ingen kontaktopplysninger med gitt saksnummer/orgnummer");
        }
        return Response.ok().build();
    }
}
