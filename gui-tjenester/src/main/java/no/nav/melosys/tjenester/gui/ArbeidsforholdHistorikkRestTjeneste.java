package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.FagsakService;
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

@Api(tags = {"arbeidsforholdhistorikk"})
@Path("/arbeidsforholdhistorikk")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class ArbeidsforholdHistorikkRestTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ArbeidsforholdHistorikkRestTjeneste.class);

    private FagsakService fagsakService;

    @Autowired
    public ArbeidsforholdHistorikkRestTjeneste(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
    }

    @GET
    @Path("{arbeidsforholdsID}")
    public Response hentArbeidsforholdHistorikk(@PathParam("arbeidsforholdsID") Long arbeidsforholdsID) {
        try {
            ArbeidsforholdDokument dokument = fagsakService.hentArbeidsforholdHistorikk(arbeidsforholdsID);
            return Response.ok(dokument).build();
        } catch (SikkerhetsbegrensningException sikkerhetsbegrensningException) {
            log.error("SikkerhetsbegrensningException under oppslag av arbeidsforhold: ", sikkerhetsbegrensningException);
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (IntegrasjonException integrasjonException) {
            log.error("Uventet IntegrasjonException under oppslag av arbeidsforhold: ", integrasjonException);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
