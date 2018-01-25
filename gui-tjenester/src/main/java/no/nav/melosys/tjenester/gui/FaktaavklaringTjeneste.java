package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"faktaavklaring"})
@Path("/faktaavklaring")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class FaktaavklaringTjeneste extends RestTjeneste {

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Utfører faktaavklaring på en gitt søknad")
    public Response utførFaktaavklaring(@PathParam("behandlingID") long behandlingID) {
        // TODO Mock. Venter på avklaringer
        String jsonFaktaAvklaring = "{\n" +
                "  \"behandlingId\": 123456789,\n" +
                "  \"faktaavklaring\": {\n" +
                "    \"periode\": {\n" +
                "      \"land\": [],\n" +
                "      \"periodeFraOgMed\": null,\n" +
                "      \"periodeTilOgMed\": null\n" +
                "    },\n" +
                "    \"aktivitet\": {\n" +
                "      \"aktivitetLand\": []\n" +
                "    },\n" +
                "    \"sysselsetting\": {\n" +
                "      \"sysselsettingType\": \"\"\n" +
                "    },\n" +
                "    \"utsending\": {\n" +
                "      \"ansattINorskSelskap\": null,\n" +
                "      \"erstatterTidligereUtsendt\": null,\n" +
                "      \"utsendingMindreEnn24Mnd\": null\n" +
                "    },\n" +
                "    \"sektor\": {\n" +
                "      \"ansattISektor\": \"\"\n" +
                "    },\n" +
                "    \"virksomhet\": {\n" +
                "      \"antallLand\": \"\",\n" +
                "      \"aktivitetINorge\": \"\",\n" +
                "      \"marginaltArbeid\": \"\",\n" +
                "      \"vekslingMellomLand\": \"\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"status\": {\n" +
                "    \"periode\": \"OK\",\n" +
                "    \"aktivitet\": \"OK\",\n" +
                "    \"sysselsetting\": \"OK\",\n" +
                "    \"utsending\": \"OK\",\n" +
                "    \"sektor\": \"OK\",\n" +
                "    \"virksomhet\": \"OK\"\n" +
                "  }\n" +
                "}";

        return Response.ok().entity(jsonFaktaAvklaring).build();
    }
}
