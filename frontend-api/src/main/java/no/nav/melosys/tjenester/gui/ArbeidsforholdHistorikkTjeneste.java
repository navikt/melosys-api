package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
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
    @ApiOperation(value = "Hent historikk arbeidsforhold", response = ArbeidsforholdDokument.class)
    public Response hentArbeidsforholdHistorikk(@ApiParam @PathParam("arbeidsforholdsID") Long arbeidsforholdsID) {
        try {
            ArbeidsforholdDokument dokument = saksopplysningerService.hentArbeidsforholdHistorikk(arbeidsforholdsID);
            return Response.ok(dokument).build();
        } catch (SikkerhetsbegrensningException e) {
            log.info("SikkerhetsbegrensningException under oppslag av arbeidsforhold", e);
            throw new ForbiddenException(e.getMessage());
        } catch (IntegrasjonException e) {
            log.error("Uventet IntegrasjonException under oppslag av arbeidsforhold", e);
            throw new InternalServerErrorException(e.getMessage());
        } catch (IkkeFunnetException e) {
            throw new NotFoundException(e.getMessage());
        }
    }
}
