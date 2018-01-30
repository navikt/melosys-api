package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.regler.api.lovvalg.rep.Argument;
import no.nav.melosys.regler.api.lovvalg.rep.Artikkel;
import no.nav.melosys.regler.api.lovvalg.rep.Betingelse;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.rep.Lovvalgsbestemmelse;
import no.nav.melosys.regler.api.lovvalg.rep.Resultat;
import no.nav.melosys.service.RegelmodulService;
import no.nav.melosys.tjenester.gui.dto.LovvalgDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"vurdering"})
@Path("/vurdering")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class VurderingTjeneste extends RestTjeneste {

    private RegelmodulService regelmodulService;

    private boolean mocking = true;

    @Autowired
    public VurderingTjeneste(RegelmodulService regelmodulService) {
        this.regelmodulService = regelmodulService;
    }

    @GET
    @Path("{behandlingID}")
    public Response regelModulKall(@PathParam("behandlingID") long behandlingID) {

        // FIXME Mocking fordi vi mangler en kjørende regelmotor
        if (mocking) {
            FastsettLovvalgReply mockFastsettLovvalgReply = mockKall();
            LovvalgDto mockLovvalgDto = new LovvalgDto(behandlingID, mockFastsettLovvalgReply);
            return Response.ok(mockLovvalgDto).build();
        }

        FastsettLovvalgReply fastsettLovvalgReply = regelmodulService.fastsettLovvalg(behandlingID);

        if (fastsettLovvalgReply == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            LovvalgDto lovvalgDto = new LovvalgDto(behandlingID, fastsettLovvalgReply);
            return Response.ok(lovvalgDto).build();
        }
    }

    public FastsettLovvalgReply mockKall() {
        FastsettLovvalgReply mockFastsettLovvalgReply = new FastsettLovvalgReply();
        mockFastsettLovvalgReply.lovvalgsbestemmelser = new ArrayList<>();

        Lovvalgsbestemmelse lovvalgsbestemmelse = new Lovvalgsbestemmelse();
        mockFastsettLovvalgReply.lovvalgsbestemmelser.add(lovvalgsbestemmelse);

        lovvalgsbestemmelse.artikkel = Artikkel.ART_12_1;

        lovvalgsbestemmelse.betingelser = new ArrayList<>();
        Betingelse betingelse = new Betingelse();
        betingelse.krav = Argument.A1_12_1_VIRKSOMHET_I_UTSENDERLAND;
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        betingelse = new Betingelse();
        betingelse.krav = Argument.A1_12_1_SENDES_TIL_ANNEN_MEDLEMSSTAT;
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        betingelse = new Betingelse();
        betingelse.krav = Argument.A1_12_1_UTENLANDSOPPHOLDET_ER_IKKE_OVER_24_MND;
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        betingelse = new Betingelse();
        betingelse.krav = Argument.A1_12_1_SKAL_ERSTATTE_ANNEN_PERSON;
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        return mockFastsettLovvalgReply;
    }
}
