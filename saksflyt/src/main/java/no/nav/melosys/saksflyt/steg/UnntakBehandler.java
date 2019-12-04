package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.saksflyt.Prosessinstans;

/**
 * Grensesnitt for alle unntakbehdlere
 */
public interface UnntakBehandler {

    void behandleUnntak(Prosessinstans prosessinstans, String melding, Throwable t);

}
