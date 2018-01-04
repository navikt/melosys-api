package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.service.RegelmotorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Api(tags = {"regelmotor"})
@Path("/regelmotor")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class RegelmotorTjeneste {

    private RegelmotorService regelmotorService;

    @Autowired
    public RegelmotorTjeneste(RegelmotorService regelMotorService) {
        this.regelmotorService = regelMotorService;
    }

    @GET
    @Path("{behandlingID}")
    public Response regelMotorKall(@PathParam("behandlingID") Long behandlingID) {
        FastsettLovvalgReply fastsettLovvalgReply = regelmotorService.fastsettLovvalg(behandlingID);
        if (fastsettLovvalgReply == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (fastsettLovvalgReply.feilmeldinger != null && !fastsettLovvalgReply.feilmeldinger.isEmpty()) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(fastsettLovvalgReply).build();
        } else {
            return Response.ok(fastsettLovvalgReply).build();
        }
    }

}
