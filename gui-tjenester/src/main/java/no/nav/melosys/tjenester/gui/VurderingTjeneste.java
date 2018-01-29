package no.nav.melosys.tjenester.gui;

import static no.nav.melosys.regler.api.lovvalg.rep.Argument.*;

import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import no.nav.melosys.regler.api.lovvalg.rep.Artikkel;
import no.nav.melosys.regler.api.lovvalg.rep.Betingelse;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.rep.Lovvalgsbestemmelse;
import no.nav.melosys.regler.api.lovvalg.rep.Resultat;
import no.nav.melosys.service.RegelmodulService;
import no.nav.melosys.tjenester.gui.dto.LovvalgDto;

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
        mockFastsettLovvalgReply.lovvalgsbestemmelser = new HashMap<>();

        Lovvalgsbestemmelse lovvalgsbestemmelse = new Lovvalgsbestemmelse();
        mockFastsettLovvalgReply.lovvalgsbestemmelser.put(Artikkel.ART_12_1, lovvalgsbestemmelse);

        lovvalgsbestemmelse.artikkel = Artikkel.ART_12_1;

        lovvalgsbestemmelse.betingelser = new ArrayList<>();
        Betingelse betingelse = new Betingelse();
        betingelse.argument = ARBEIDSPLASSEN_I_UTLANDET_DEKKES_AV_EF_883_2004;
        betingelse.krav = "Arbeidsplassen i utlandet er på et sted som dekkes av EF 883/2004 er sann";
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        betingelse = new Betingelse();
        betingelse.argument = BRUKER_HAR_NORSK_ARBEIDSGIVER;
        betingelse.krav = "Bruker har norsk arbeidsgiver er sann";
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        betingelse = new Betingelse();
        betingelse.argument = ANTALL_ARBEIDSGIVERE_I_SØKNADSPERIODEN;
        betingelse.krav = "Antall arbeidsgivere i søknadsperioden er lik 1";
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        betingelse = new Betingelse();
        betingelse.argument = HOVEDARBEIDSFORHOLDET_VARER_I_HELE_SØKNADSPERIODEN;
        betingelse.krav = "Hovedarbeidsforholdet varer i hele søknadsperioden er sann";
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        betingelse = new Betingelse();
        betingelse.argument = LENGDE_MND_UTENLANDSOPPHOLD;
        betingelse.krav = "Antall måneder utenlandsoppholdet varer er mindre enn eller lik 24";
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        betingelse = new Betingelse();
        betingelse.argument = BRUKER_ER_MEDLEM_AV_FTR_MÅNEDEN_FØR_PERIODESTART;
        betingelse.krav = "Bruker er medlem av FTR måneden før periodestart er sann";
        betingelse.resultat = Resultat.OPPFYLT;
        lovvalgsbestemmelse.betingelser.add(betingelse);

        return mockFastsettLovvalgReply;
    }
}
