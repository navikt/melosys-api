package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.service.RegelmodulService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Api(tags = {"regelmodul"})
@Path("/regelmodul")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class RegelmodulTjeneste {

    private RegelmodulService regelmodulService;

    @Autowired
    public RegelmodulTjeneste(RegelmodulService regelmodulService) {
        this.regelmodulService = regelmodulService;
    }

    @GET
    @Path("{behandlingID}")
    public Response regelModulKall(@PathParam("behandlingID") Long behandlingID) {
        FastsettLovvalgReply fastsettLovvalgReply = regelmodulService.fastsettLovvalg(behandlingID);
        if (fastsettLovvalgReply == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(fastsettLovvalgReply).build();
    }

}
