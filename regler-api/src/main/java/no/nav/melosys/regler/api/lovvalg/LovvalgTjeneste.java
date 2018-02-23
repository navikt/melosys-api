package no.nav.melosys.regler.api.lovvalg;

import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;

/**
 * Grensesnitt for tjenester relatert til lovvalg
 */
public interface LovvalgTjeneste {

    /**
     * Fastsetter lovvalgsland for en forespørsel.
     * @param xml Forespørselen som XML
     * @return Reply som inneholder bl.a. fastsatt lovartikkel
     */
    public FastsettLovvalgReply fastsettLovvalgApi(String xml);

}
