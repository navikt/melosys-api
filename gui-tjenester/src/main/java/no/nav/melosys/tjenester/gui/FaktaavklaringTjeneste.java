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
                "      \"land\": [\"FR\"],\n" +
                "      \"periodeFraOgMed\": \"2018-01-01\",\n" +
                "      \"periodeTilOgMed\": \"2019-01-01\"\n" +
                "    },\n" +
                "    \"aktivitet\": {\n" +
                "      \"aktivitetLand\": [\"DE\"]\n" +
                "    },\n" +
                "    \"sysselsetting\": {\n" +
                "      \"sysselsettingType\": \"ARBEIDSTAKER\"\n" +
                "    },\n" +
                "    \"utsending\": {\n" +
                "      \"ansattINorskSelskap\": true,\n" +
                "      \"erstatterTidligereUtsendt\": false,\n" +
                "      \"utsendingMindreEnn24Mnd\": true\n" +
                "    },\n" +
                "    \"bostedsland\": {\n" +
                "      \"bekrefterFamiliebosted\": null,\n" +
                "      \"bekrefterDisponering\": null,\n" +
                "      \"bostedsLand\": []\n" +
                "    },\n" +
                "    \"sektor\": {\n" +
                "      \"ansattISektor\": \"INGEN_AV_DISSE\"\n" +
                "    },\n" +
                "    \"virksomhet\": {\n" +
                "      \"antallLand\": \"ETT_LAND_IKKE_NORGE\",\n" +
                "      \"aktivitetINorge\": \"OVER_25_PROSENT\",\n" +
                "      \"marginaltArbeid\": \"MARGINALT_JA\",\n" +
                "      \"vekslingMellomLand\": \"EN_ELLER_BEGGE\"\n" +
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
