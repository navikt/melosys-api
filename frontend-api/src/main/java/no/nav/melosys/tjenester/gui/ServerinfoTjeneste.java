package no.nav.melosys.tjenester.gui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        serverinfoDto = new ServerinfoDto(
            System.getenv(NAMESPACE_ENV),
            System.getenv(CLUSTER_ENV),
            hentBranch(),
            hentHash()
        );
    }

    private static List<String> hentBranchOgHash() {
        String image = System.getenv(IMAGE_ENV);

        if (StringUtils.isNotEmpty(image) && image.split(":").length == 3) {
            String imageTag = image.split(":")[2];

            if (StringUtils.isNotEmpty(imageTag)) {
                return Arrays.asList(imageTag.split("-"));
            }
        }

        return Collections.emptyList();
    }

    private static String hentBranch() {
        List<String> branchOgHash = hentBranchOgHash();

        if (branchOgHash.size() == 2) {
            return branchOgHash.get(0);
        }

        return null;
    }

    private static String hentHash() {
        List<String> branchOgHash = hentBranchOgHash();

        if (branchOgHash.size() == 2) {
            return branchOgHash.get(1);
        }

        return null;
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
