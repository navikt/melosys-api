package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.SaksopplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"arbeidsforholdhistorikk"})
@Path("/arbeidsforholdhistorikk")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class ArbeidsforholdHistorikkTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(ArbeidsforholdHistorikkTjeneste.class);

    private SaksopplysningerService saksopplysningerService;

    @Autowired
    public ArbeidsforholdHistorikkTjeneste(SaksopplysningerService saksopplysningerService) {
        this.saksopplysningerService = saksopplysningerService;
    }

    @GET
    @Path("{arbeidsforholdsID}")
    public Response hentArbeidsforholdHistorikk(@PathParam("arbeidsforholdsID") Long arbeidsforholdsID) {
        try {
            ArbeidsforholdDokument dokument = saksopplysningerService.hentArbeidsforholdHistorikk(arbeidsforholdsID);
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
