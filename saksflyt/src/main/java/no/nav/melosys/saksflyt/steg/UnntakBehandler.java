package no.nav.melosys.saksflyt.steg;

import no.nav.melosys.domain.saksflyt.Prosessinstans;

public interface UnntakBehandler {

    void behandleUnntak(Prosessinstans prosessinstans, String melding, Throwable t);

}
