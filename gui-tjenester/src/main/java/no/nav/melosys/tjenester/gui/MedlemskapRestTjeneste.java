package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.integrasjon.medl2.Medl2Fasade;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Medlemsperiode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.AbstractResource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;

@Api(tags = {"medlemskap"})
@Path("/medlemskap")
@Service
@Scope(WebApplicationContext.SCOPE_REQUEST)
public class MedlemskapRestTjeneste extends RestTjeneste {

    private static Logger log = LoggerFactory.getLogger(MedlemskapRestTjeneste.class);

    private Medl2Fasade medl2;

    @Autowired
    public MedlemskapRestTjeneste(Medl2Fasade medl2) {
        this.medl2 = medl2;
    }

    @GET
    @Path("{ident}")
    @ApiOperation("Søk etter medlemskap på personnummer")
    public Response hentPeriodeListe(@PathParam("ident") String fnr) {
        try {
            List<Medlemsperiode> medlemskap = medl2.hentPeriodeListe(fnr);
            return Response.ok(medlemskap).build();
        } catch (PersonIkkeFunnet e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Sikkerhetsbegrensning e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }
}
