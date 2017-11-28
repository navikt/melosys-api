package no.nav.melosys.regler.api.lovvalg;

import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.req.FastsettLovvalgRequest;

/**
 * Grensesnitt for tjenester relatert til lovvalg
 */
public interface LovvalgTjeneste {
    
    /**
     * Fastsetter lovvalgsland for en forespørsel.
     * @param req Forespørselen
     * @return Reply som inneholder bl.a. fastsatt lovartikkel
     */
    public FastsettLovvalgReply fastsettLovvalg(FastsettLovvalgRequest req);

}
