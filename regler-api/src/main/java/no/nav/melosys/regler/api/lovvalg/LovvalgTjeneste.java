package no.nav.melosys.regler.api.lovvalg;

/**
 * Grensesnitt for tjenester relatert til lovvalg
 */
public interface LovvalgTjeneste {
    
    /**
     * Fastsetter lovvalgsland for en forespørsel.
     * @param req Forespørselen
     * @return Respons som inneholder bl.a. fastsatt lovvalgsland og lovartikkel
     */
    public FastsettLovvalgRespons fastsettLovvalg(FastsettLovvalgRequest req);

}
