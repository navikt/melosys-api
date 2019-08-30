package no.nav.melosys.tjenester.gui.serverinfo;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.tjenester.gui.dto.ServerinfoDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"serverinfo"})
@Service
@Path("/serverinfo")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ServerinfoTjeneste {
    @GET
    @ApiOperation(
        value = "Henter informasjon om miljø og bygg av backend.",
        response = ServerinfoDto.class
    )
    public Response hentServerStatus() {
        return Response.ok(Serverinfo.tilDto()).build();
    }
}
