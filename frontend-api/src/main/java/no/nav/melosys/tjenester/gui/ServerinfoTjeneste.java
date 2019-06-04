package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.tjenester.gui.dto.ServerinfoDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"serverinfo"})
@Service
@Path("/serverinfo")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ServerinfoTjeneste {

    private static final String NAMESPACE_ENV = "NAIS_NAMESPACE";
    private static final String CLUSTER_ENV = "NAIS_CLUSTER_NAME";
    private static final String IMAGE_ENV = "NAIS_APP_IMAGE";

    private static ServerinfoDto serverinfoDto;

    static {
        serverinfoDto = new ServerinfoDto();
        serverinfoDto.setNamespace(System.getenv(NAMESPACE_ENV));
        serverinfoDto.setCluster(System.getenv(CLUSTER_ENV));
        serverinfoDto = settBranchOgHash(serverinfoDto);
    }

    private static ServerinfoDto settBranchOgHash(ServerinfoDto serverinfoDto) {
        String image = System.getenv(IMAGE_ENV);

        if (StringUtils.isNotEmpty(image) && image.split(":").length == 3) {
            String imageTag = image.split(":")[2];

            if (StringUtils.isNotEmpty(imageTag)) {
                String[] branchAndCommit = imageTag.split("-");

                if (branchAndCommit.length == 2) {
                    serverinfoDto.setBranchName(branchAndCommit[0]);
                    serverinfoDto.setLongVersionHash(branchAndCommit[1]);
                }
            }
        }

        return serverinfoDto;
    }

    @GET
    @ApiOperation(
        value = "Henter informasjon om miljø og bygg av backend.",
        response = ServerinfoDto.class
    )
    public Response hentServerStatus() {
        return Response.ok(serverinfoDto).build();
    }
}
