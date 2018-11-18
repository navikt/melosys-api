package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.SaksopplysningerService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"arbeidsforholdhistorikk"})
@Path("/arbeidsforholdhistorikk")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class ArbeidsforholdHistorikkTjeneste extends RestTjeneste {

    private SaksopplysningerService saksopplysningerService;

    @Autowired
    public ArbeidsforholdHistorikkTjeneste(SaksopplysningerService saksopplysningerService) {
        this.saksopplysningerService = saksopplysningerService;
    }

    @GET
    @Path("{arbeidsforholdsID}")
    @ApiOperation(value = "Hent historikk arbeidsforhold", response = ArbeidsforholdDokument.class)
    public Response hentArbeidsforholdHistorikk(@ApiParam @PathParam("arbeidsforholdsID") Long arbeidsforholdsID) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
            ArbeidsforholdDokument dokument = saksopplysningerService.hentArbeidsforholdHistorikk(arbeidsforholdsID);
            return Response.ok(dokument).build();
    }

}
