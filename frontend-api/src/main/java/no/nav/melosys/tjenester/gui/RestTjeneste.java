package no.nav.melosys.tjenester.gui;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Abstract klasse som brukes til å sette MediaType og charset til alle GUI tjenester.
 * @see java.lang.annotation.Inherited
 */
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON + "; charset=UTF-8")
public abstract class RestTjeneste {
    static final String TOM_JSON = "{}";

    String tomJson() {
        return TOM_JSON;
    }
}
