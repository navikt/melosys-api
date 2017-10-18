package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.medl2.Medl2Fasade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

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
    public Response hentMedlemsperiodeListe(@PathParam("ident") String fnr) {
        try {
            Saksopplysning saksopplysning = medl2.getPeriodeListe(fnr);
            return Response.ok().build();
        } catch (IntegrasjonException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }
}
