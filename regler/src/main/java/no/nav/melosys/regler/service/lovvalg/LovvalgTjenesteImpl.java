package no.nav.melosys.regler.service.lovvalg;

import java.util.ArrayList;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.swagger.annotations.*;
import no.nav.melosys.regler.api.lovvalg.LovvalgTjeneste;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.rep.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.rep.Kategori;
import no.nav.melosys.regler.api.lovvalg.req.FastsettLovvalgRequest;
import no.nav.melosys.regler.lovvalg.LovvalgRegelflyt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.regler.lovvalg.LovvalgKontekstManager.*;

@Component
@Path("lovvalg")
@Api
@SwaggerDefinition(
        basePath = "lovvalg",
        info = @Info(
                title = "lovvalg",
                version = "0",
                contact = @Contact(
                        name = "Team MELOSYS"
                ),
                description = "Tjenester for å fastsette lovvalg" 
        ),
        consumes = {LovvalgTjenesteImpl.APPLICATION_JSON_UTF_8},
        produces = {LovvalgTjenesteImpl.APPLICATION_JSON_UTF_8},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS}
)
public class LovvalgTjenesteImpl implements LovvalgTjeneste {
    
    // Denne kan flyttes til en felles-util modul
    public static final String APPLICATION_JSON_UTF_8 = "application/json;charset=utf-8";
    
    private static Logger log = LoggerFactory.getLogger(LovvalgTjenesteImpl.class);

    @Override
    @POST
    @Path("fastsettLovvalg") // FIXME: Denne tjenesten er nært bundet til søknad a1. Bør gjenspeiles i tjenestenavn.
    @Consumes(LovvalgTjenesteImpl.APPLICATION_JSON_UTF_8)
    @Produces(LovvalgTjenesteImpl.APPLICATION_JSON_UTF_8)
    @ApiOperation(
            value= "Fastsetter lovvalgsland",
            notes = "Tjeneste som anvender lovverk til å fastsette lovvalgsland for en forespørsel"
    )
    public FastsettLovvalgReply fastsettLovvalg(FastsettLovvalgRequest req) {
        try {
            // Sett lokal kontekst for regelsett...
            initialiserLokalKontekst(req);
            // Kjør forretningsregler og returner respons...
            new LovvalgRegelflyt().kjør();
            return responsen();
        } catch (Throwable e) {
            // Forsok å logge feilen...
            try {
                log.error("Uventet Exception", e);
            } catch (Throwable ignored) {
            }
            // Returner teknisk feil...
            FastsettLovvalgReply res = new FastsettLovvalgReply();
            res.feilmeldinger = new ArrayList<>();
            res.lovvalgsbestemmelser = new ArrayList<>();

            Feilmelding feil = new Feilmelding();
            feil.kategori = Kategori.TEKNISK_FEIL;
            feil.feilmelding = "Uventet Exception";
            res.feilmeldinger.add(feil);
            // Forsøk å legge til evt. andre feil også...
            try {
                res.feilmeldinger.addAll(responsen().feilmeldinger);
            } catch (Throwable ignored) {
            }
            return res;
        } finally {
            // Fjern lokal kontekst...
            try {
                slettLokalKontekst();
            } catch (Throwable ignored) {
            }
        }
    }

}
