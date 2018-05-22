package no.nav.melosys.tjenester.gui;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.service.dokumentmottak.ForsendelsesinformasjonDto;
import no.nav.melosys.service.dokumentmottak.ProsessinstansMeldingsfordeler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"mottak"}, description = "KUN FOR TEST")
@Path("/mottak")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class DokMotQueueTestRestTjeneste extends RestTjeneste {

    private ProsessinstansMeldingsfordeler meldingsfordeler;

    @Autowired
    public DokMotQueueTestRestTjeneste(ProsessinstansMeldingsfordeler meldingsfordeler) {
        this.meldingsfordeler = meldingsfordeler;
    }

    @POST
    @Path("/melding")
    @Consumes(MediaType.APPLICATION_XML)
    @Deprecated
    public Response mottaTynnmeldingFraTestHub(ForsendelsesinformasjonDto forsendelsesinformasjonDto) {
        meldingsfordeler.execute(forsendelsesinformasjonDto);
        return Response.ok().build();
    }
}
