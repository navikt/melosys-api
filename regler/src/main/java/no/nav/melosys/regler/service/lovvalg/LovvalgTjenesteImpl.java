package no.nav.melosys.regler.service.lovvalg;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBResult;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;

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
    
    // Disse kan flyttes til en felles-util modul
    public static final String APPLICATION_JSON_UTF_8 = "application/json;charset=utf-8";
    public static final String APPLICATION_XML_UTF_8 = "application/xml;charset=utf-8";

    private static Logger log = LoggerFactory.getLogger(LovvalgTjenesteImpl.class);

    private final JAXBContext context;

    private final Transformer transformer;

    public LovvalgTjenesteImpl() {
        final String resource = "fastsett-lovvalg-request.xslt";

        try (InputStream xslt = getClass().getClassLoader().getResourceAsStream(resource)) {
            final Source source = new StreamSource(xslt);
            context = JAXBContext.newInstance(FastsettLovvalgRequest.class);
            transformer = TransformerFactory.newInstance().newTransformer(source);
        } catch (IOException | JAXBException | TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @POST
    @Path("fastsettLovvalg") // FIXME: Denne tjenesten er nært bundet til søknad a1. Bør gjenspeiles i tjenestenavn.
    @Consumes(LovvalgTjenesteImpl.APPLICATION_XML_UTF_8)
    @Produces(LovvalgTjenesteImpl.APPLICATION_JSON_UTF_8)
    @ApiOperation(
            value= "Fastsetter lovvalgsland",
            notes = "Tjeneste som anvender lovverk til å fastsette lovvalgsland for en forespørsel"
    )
    public FastsettLovvalgReply fastsettLovvalgApi(String xml) {
        try {
            JAXBResult result = new JAXBResult(context);
            StringReader reader = new StringReader(xml);
            transformer.transform(new StreamSource(reader), result);
            FastsettLovvalgRequest request = (FastsettLovvalgRequest) result.getResult();
            return fastsettLovvalg(request);
        } catch (JAXBException | TransformerException e) {
            log.error("", e);
        }
        return null;
    }

    public FastsettLovvalgReply fastsettLovvalg(FastsettLovvalgRequest req) {
        try {
            // Sett lokal kontekst for regelsett...
            initialiserLokalKontekst(req);
            // Kjør forretningsregler og returner respons...
            LovvalgRegelflyt.getInstanse().kjør();
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

            Feilmelding feil = new Feilmelding();
            feil.kategori = Kategori.TEKNISK_FEIL;
            feil.melding = "Uventet Exception";
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
