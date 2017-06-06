package no.nav.melosys.regler.service.lovvalg;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRequest;
import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRespons;
import no.nav.melosys.regler.api.lovvalg.LovvalgTjeneste;
import no.nav.melosys.regler.lovvalg.FastsettLovvalg;

@Component
@Path("Lovvalg")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api
@SwaggerDefinition(
        basePath = "Lovvalg",
        info = @Info(
                title = "Lovvalg",
                version = "0",
                contact = @Contact(
                        name = "Team MELOSYS"
                ),
                description = "Tjenester for å fastsette lovvalg" 
        ),
        consumes = {MediaType.APPLICATION_JSON},
        produces = {MediaType.APPLICATION_JSON},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS}
)
public class LovvalgTjenesteImpl implements LovvalgTjeneste {

    @Override
    @GET
    @Path("fastsettLovvalg")
    @ApiOperation(
            value= "Fastsetter lovvalgsland",
            notes = "Tjeneste som anvender lovverk til å fastsette lovvalgsland for en forespørsel"
    )
    public FastsettLovvalgRespons fastsettLovvalg(FastsettLovvalgRequest req) {
        return FastsettLovvalg.fastsettLovvalg(req);
    }

}
