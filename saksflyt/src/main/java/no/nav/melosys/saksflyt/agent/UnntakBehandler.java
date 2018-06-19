package no.nav.melosys.saksflyt.agent;

import no.nav.melosys.domain.Prosessinstans;

/**
 * Grensesnitt for alle unntakbehdlere
 */
public interface UnntakBehandler {
    
    void behandleUnntak(Prosessinstans prosessinstans, Throwable t);

}
