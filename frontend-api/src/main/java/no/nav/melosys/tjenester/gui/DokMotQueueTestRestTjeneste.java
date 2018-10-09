package no.nav.melosys.tjenester.gui;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.integrasjon.dokumentmottak.ForsendelsesinformasjonDto;
import no.nav.melosys.service.DokMotQueueTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

// FIXME: Brukes kun til test -- skal ikke ut i prod
@Profile("test")
@Api(tags = {"mottak"}, description = "KUN FOR TEST")
@Path("/mottak")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DokMotQueueTestRestTjeneste extends RestTjeneste {

    private DokMotQueueTestService service;

    @Autowired
    public DokMotQueueTestRestTjeneste(DokMotQueueTestService service) {
        this.service = service;
    }

    @POST
    @Path("/melding")
    @ApiOperation(value = "motta tynnmelding fra test hub")
    @Consumes(MediaType.APPLICATION_XML)
    @Deprecated
    public Response mottaTynnmeldingFraTestHub(@ApiParam ForsendelsesinformasjonDto forsendelsesinformasjonDto) {
        service.mottaTynnmeldingFraTestHub(forsendelsesinformasjonDto);
        return Response.ok().build();
    }

}
