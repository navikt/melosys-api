package no.nav.melosys.tjenester.gui;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Api(tags = {"adresser"})
@Path("/adresser")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AdresseTjeneste extends RestTjeneste {

    private final UtenlandskMyndighetRepository utenlandskMyndighetRepo;

    public AdresseTjeneste(UtenlandskMyndighetRepository utenlandskMyndighetRepo) {
        this.utenlandskMyndighetRepo = utenlandskMyndighetRepo;
    }

    @GET
    @Path("/myndigheter/{landkode}")
    @ApiOperation(
        value = "Henter adressen til en gitt utenlandsk myndighet",
        response = UtenlandskMyndighet.class)
    public Response hentMyndighet(@PathParam("landkode") Landkoder landkode) {
        Optional<UtenlandskMyndighet> utenlandskMyndighet = utenlandskMyndighetRepo.findByLandkode(landkode);
        return utenlandskMyndighet.map(Response::ok).orElse(Response.status(NOT_FOUND)).build();
    }

    @GET
    @Path("/myndigheter")
    @ApiOperation(
        value = "Henter adresser til alle utenlandske myndigheter",
        response = UtenlandskMyndighet.class,
        responseContainer = "List")
    public Response hentMyndigheter() {
        List<UtenlandskMyndighet> utenlandskeMyndigheter = utenlandskMyndighetRepo.findAll();
        return Response.ok(utenlandskeMyndigheter).build();
    }
}