package no.nav.melosys.regler.service.lovvalg;

<<<<<<< HEAD
import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.initialiserLokalKontekst;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.respons;
import static no.nav.melosys.regler.lovvalg.LovvalgKontekst.slettLokalKontekst;

=======
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

<<<<<<< HEAD
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
=======
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.SwaggerDefinition;
import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRequest;
import no.nav.melosys.regler.api.lovvalg.FastsettLovvalgRespons;
<<<<<<< HEAD
import no.nav.melosys.regler.api.lovvalg.Feilmelding;
import no.nav.melosys.regler.api.lovvalg.Kategori;
=======
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
import no.nav.melosys.regler.api.lovvalg.LovvalgTjeneste;
import no.nav.melosys.regler.lovvalg.FastsettLovvalg;

@Component
@Path("Lovvalg")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api
@SwaggerDefinition(
        basePath = "Lovvalg",
        info = @Info(
                title = "Lovvalg",
                version = "0",
                contact = @Contact(
                        name = "Team MELOSYS"
                ),
                description = "Tjenester for å fastsette lovvalg" 
        ),
        consumes = {MediaType.APPLICATION_JSON},
        produces = {MediaType.APPLICATION_JSON},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS}
)
public class LovvalgTjenesteImpl implements LovvalgTjeneste {
<<<<<<< HEAD
    
    private static Logger log = LoggerFactory.getLogger(LovvalgTjenesteImpl.class);
=======
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb

    @Override
    @GET
    @Path("fastsettLovvalg")
    @ApiOperation(
            value= "Fastsetter lovvalgsland",
            notes = "Tjeneste som anvender lovverk til å fastsette lovvalgsland for en forespørsel"
    )
    public FastsettLovvalgRespons fastsettLovvalg(FastsettLovvalgRequest req) {
<<<<<<< HEAD
        try {
            // Sett lokal kontekst for regelsett...
            initialiserLokalKontekst(req);
            // Kjør forretningsregler og returner respons...
            new FastsettLovvalg().kjørRegler();
            return respons();
        } catch(Throwable e) {
            // Forsok å logge feilen...
            try {
                log.error("Uventet Exception", e);
            } catch (Throwable ignored) {
            }
            // Returner teknisk feil...
            FastsettLovvalgRespons res = new FastsettLovvalgRespons();
            Feilmelding feil = new Feilmelding();
            feil.kategori = Kategori.TEKNISK_FEIL;
            feil.feilmelding = "Uventet Exception";
            res.feilmeldinger.add(feil);
            // Forsøk å legge til evt. andre feil også...
            try {
                res.feilmeldinger.addAll(respons().feilmeldinger);
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
=======
        return FastsettLovvalg.fastsettLovvalg(req);
>>>>>>> cf2df1bd3807d09d0dbf434f50c32f1e049fa5fb
    }

}
